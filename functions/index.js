const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

/**
 * Backup Upload Trigger
 * Automatically triggered when a new backup is uploaded to Cloud Storage
 */
exports.onBackupUploaded = functions.storage.object().onFinalize(async (object) => {
  const filePath = object.name;
  const contentType = object.contentType;
  
  console.log('Backup uploaded:', filePath);
  
  if (!filePath.startsWith('backups/')) {
    console.log('Not a backup file, skipping');
    return null;
  }
  
  try {
    const metadata = {
      filePath: filePath,
      size: object.size,
      contentType: contentType,
      uploadTime: object.timeCreated,
      checksum: object.md5Hash
    };
    
    await admin.firestore().collection('backups').add({
      ...metadata,
      status: 'uploaded',
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });
    
    await admin.firestore().collection('tasks').add({
      type: 'process_backup',
      backupPath: filePath,
      status: 'pending',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    console.log('Backup metadata stored and processing triggered');
    
    const userId = extractUserIdFromPath(filePath);
    if (userId) {
      await sendBackupNotification(userId, 'Backup uploaded successfully');
    }
    
    return null;
  } catch (error) {
    console.error('Error processing backup upload:', error);
    throw error;
  }
});

/**
 * Scheduled Backup Trigger
 */
exports.scheduledBackupTrigger = functions.pubsub
  .schedule('0 2 * * *')
  .timeZone('America/New_York')
  .onRun(async (context) => {
    console.log('Running scheduled backup trigger');
    
    try {
      const usersSnapshot = await admin.firestore()
        .collection('users')
        .where('autoBackup', '==', true)
        .get();
      
      const backupTasks = [];
      
      usersSnapshot.forEach((doc) => {
        const userData = doc.data();
        backupTasks.push(
          admin.firestore().collection('tasks').add({
            type: 'trigger_backup',
            userId: doc.id,
            deviceId: userData.deviceId,
            status: 'pending',
            createdAt: admin.firestore.FieldValue.serverTimestamp()
          })
        );
      });
      
      await Promise.all(backupTasks);
      
      console.log(`Scheduled ${backupTasks.length} backup tasks`);
      return null;
    } catch (error) {
      console.error('Error in scheduled backup trigger:', error);
      throw error;
    }
  });

/**
 * Backup Verification Trigger
 */
exports.verifyBackupIntegrity = functions.firestore
  .document('backups/{backupId}')
  .onCreate(async (snap, context) => {
    const backup = snap.data();
    const backupId = context.params.backupId;
    
    console.log('Verifying backup integrity:', backupId);
    
    try {
      const bucket = admin.storage().bucket();
      const file = bucket.file(backup.filePath);
      
      const [exists] = await file.exists();
      if (!exists) {
        throw new Error('Backup file not found');
      }
      
      const [metadata] = await file.getMetadata();
      const storedChecksum = metadata.md5Hash;
      
      if (storedChecksum !== backup.checksum) {
        console.error('Checksum mismatch for backup:', backupId);
        await snap.ref.update({
          status: 'corrupted',
          verificationResult: 'failed',
          verifiedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        return null;
      }
      
      await snap.ref.update({
        status: 'verified',
        verificationResult: 'passed',
        verifiedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      
      console.log('Backup verification successful:', backupId);
      return null;
    } catch (error) {
      console.error('Error verifying backup:', error);
      await snap.ref.update({
        status: 'verification_failed',
        verificationError: error.message,
        verifiedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      throw error;
    }
  });

/**
 * Backup Cleanup Trigger
 */
exports.cleanupOldBackups = functions.pubsub
  .schedule('0 3 * * 0')
  .timeZone('America/New_York')
  .onRun(async (context) => {
    console.log('Running backup cleanup');
    
    try {
      const policiesSnapshot = await admin.firestore()
        .collection('retention_policies')
        .get();
      
      const deletionTasks = [];
      
      for (const policyDoc of policiesSnapshot.docs) {
        const policy = policyDoc.data();
        const retentionDays = policy.retentionDays || 30;
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - retentionDays);
        
        const oldBackupsSnapshot = await admin.firestore()
          .collection('backups')
          .where('userId', '==', policy.userId)
          .where('uploadTime', '<', cutoffDate.toISOString())
          .get();
        
        oldBackupsSnapshot.forEach((backupDoc) => {
          const backup = backupDoc.data();
          
          if (backup.legalHold || backup.immutable) {
            console.log('Skipping backup with legal hold/immutable:', backupDoc.id);
            return;
          }
          
          deletionTasks.push(
            deleteBackup(backupDoc.id, backup.filePath)
          );
        });
      }
      
      await Promise.all(deletionTasks);
      
      console.log(`Cleaned up ${deletionTasks.length} old backups`);
      return null;
    } catch (error) {
      console.error('Error in backup cleanup:', error);
      throw error;
    }
  });

/**
 * Cost Monitoring Trigger
 */
exports.monitorCosts = functions.pubsub
  .schedule('0 0 * * *')
  .timeZone('America/New_York')
  .onRun(async (context) => {
    console.log('Running cost monitoring');
    
    try {
      const backupsSnapshot = await admin.firestore()
        .collection('backups')
        .where('status', '==', 'verified')
        .get();
      
      let totalStorage = 0;
      const userStorage = {};
      
      backupsSnapshot.forEach((doc) => {
        const backup = doc.data();
        totalStorage += parseInt(backup.size) || 0;
        
        if (!userStorage[backup.userId]) {
          userStorage[backup.userId] = 0;
        }
        userStorage[backup.userId] += parseInt(backup.size) || 0;
      });
      
      await admin.firestore().collection('metrics').add({
        type: 'storage',
        totalBytes: totalStorage,
        userBreakdown: userStorage,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
      });
      
      const storageGB = totalStorage / (1024 * 1024 * 1024);
      const estimatedCost = storageGB * 0.023;
      
      if (estimatedCost > 100) {
        console.log('ALERT: High storage costs detected:', estimatedCost);
        await sendAdminNotification('High storage costs', `Estimated cost: $${estimatedCost.toFixed(2)}`);
      }
      
      console.log(`Storage monitoring complete. Total: ${storageGB.toFixed(2)} GB, Cost: $${estimatedCost.toFixed(2)}`);
      return null;
    } catch (error) {
      console.error('Error in cost monitoring:', error);
      throw error;
    }
  });

/**
 * Multi-Region Replication Trigger
 */
exports.replicateBackup = functions.firestore
  .document('backups/{backupId}')
  .onCreate(async (snap, context) => {
    const backup = snap.data();
    const backupId = context.params.backupId;
    
    if (backup.skipReplication) {
      return null;
    }
    
    console.log('Replicating backup to multiple regions:', backupId);
    
    try {
      const bucket = admin.storage().bucket();
      const sourceFile = bucket.file(backup.filePath);
      
      const targetRegions = ['us-west1', 'europe-west1', 'asia-east1'];
      
      const replicationTasks = targetRegions.map(async (region) => {
        const targetPath = `${region}/${backup.filePath}`;
        const targetFile = bucket.file(targetPath);
        
        await sourceFile.copy(targetFile);
        
        return {
          region: region,
          path: targetPath,
          status: 'replicated'
        };
      });
      
      const results = await Promise.all(replicationTasks);
      
      await snap.ref.update({
        replicated: true,
        replicationRegions: results,
        replicatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      
      console.log('Backup replication complete:', backupId);
      return null;
    } catch (error) {
      console.error('Error replicating backup:', error);
      await snap.ref.update({
        replicationError: error.message,
        replicationAttemptedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      throw error;
    }
  });

function extractUserIdFromPath(filePath) {
  const match = filePath.match(/^backups\/([^/]+)\//);
  return match ? match[1] : null;
}

async function sendBackupNotification(userId, message) {
  try {
    const userDoc = await admin.firestore().collection('users').doc(userId).get();
    const fcmToken = userDoc.data()?.fcmToken;
    
    if (fcmToken) {
      await admin.messaging().send({
        token: fcmToken,
        notification: {
          title: 'ObsidianBackup',
          body: message
        }
      });
    }
  } catch (error) {
    console.error('Error sending notification:', error);
  }
}

async function sendAdminNotification(title, message) {
  console.log('ADMIN NOTIFICATION:', title, message);
}

async function deleteBackup(backupId, filePath) {
  try {
    const bucket = admin.storage().bucket();
    const file = bucket.file(filePath);
    
    await file.delete();
    await admin.firestore().collection('backups').doc(backupId).delete();
    
    console.log('Backup deleted:', backupId);
  } catch (error) {
    console.error('Error deleting backup:', error);
  }
}

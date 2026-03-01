package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.verification.ChecksumVerifier
import com.obsidianbackup.verification.MerkleTree
import com.obsidianbackup.verification.MerkleVerificationEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VerificationModule {
    
    @Provides
    @Singleton
    fun provideMerkleTree(): MerkleTree {
        return MerkleTree()
    }
    
    @Provides
    @Singleton
    fun provideChecksumVerifier(merkleTree: MerkleTree): ChecksumVerifier {
        return ChecksumVerifier(merkleTree)
    }
    
    @Provides
    @Singleton
    fun provideMerkleVerificationEngine(
        merkleTree: MerkleTree,
        checksumVerifier: ChecksumVerifier,
        catalog: BackupCatalog
    ): MerkleVerificationEngine {
        return MerkleVerificationEngine(merkleTree, checksumVerifier, catalog)
    }
}

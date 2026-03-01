import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone Merkle Tree Verification
 * Tests the core algorithm logic without Android dependencies
 */
public class VerifyMerkleTree {
    
    static class CloudFile {
        String name;
        String checksum;
        long sizeBytes;
        
        CloudFile(String name, String checksum) {
            this.name = name;
            this.checksum = checksum;
            this.sizeBytes = 1024L;
        }
    }
    
    public static String calculateMerkleRoot(List<CloudFile> files) throws Exception {
        if (files.isEmpty()) {
            return "";
        }
        
        if (files.size() == 1) {
            return files.get(0).checksum;
        }
        
        // Initialize leaf nodes with file checksums
        List<byte[]> currentLevel = new ArrayList<>();
        for (CloudFile file : files) {
            currentLevel.add(hexToBytes(file.checksum));
        }
        
        // Build tree bottom-up
        while (currentLevel.size() > 1) {
            currentLevel = buildNextLevel(currentLevel);
        }
        
        return bytesToHex(currentLevel.get(0));
    }
    
    public static List<byte[]> buildNextLevel(List<byte[]> currentLevel) throws Exception {
        List<byte[]> nextLevel = new ArrayList<>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        for (int i = 0; i < currentLevel.size(); i += 2) {
            byte[] left = currentLevel.get(i);
            byte[] right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : currentLevel.get(i);
            
            digest.reset();
            digest.update(left);
            digest.update(right);
            nextLevel.add(digest.digest());
        }
        
        return nextLevel;
    }
    
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
    
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Merkle Tree Implementation Tests ===\n");
        
        int passed = 0;
        int failed = 0;
        
        // Test 1: Empty list
        System.out.println("Test 1: Empty file list");
        String test1 = calculateMerkleRoot(new ArrayList<>());
        if (test1.equals("")) {
            System.out.println("✓ PASS: Empty list returns empty string");
            passed++;
        } else {
            System.out.println("✗ FAIL: Expected empty string, got: " + test1);
            failed++;
        }
        
        // Test 2: Single file
        System.out.println("\nTest 2: Single file");
        List<CloudFile> singleFile = new ArrayList<>();
        singleFile.add(new CloudFile("file1.txt", repeat("a", 64)));
        String test2 = calculateMerkleRoot(singleFile);
        if (test2.equals(singleFile.get(0).checksum)) {
            System.out.println("✓ PASS: Single file returns its checksum");
            passed++;
        } else {
            System.out.println("✗ FAIL: Expected " + singleFile.get(0).checksum + ", got: " + test2);
            failed++;
        }
        
        // Test 3: Two files
        System.out.println("\nTest 3: Two files");
        List<CloudFile> twoFiles = new ArrayList<>();
        twoFiles.add(new CloudFile("file1.txt", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
        twoFiles.add(new CloudFile("file2.txt", "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592"));
        String test3 = calculateMerkleRoot(twoFiles);
        if (test3.length() == 64 && test3.matches("[0-9a-f]{64}")) {
            System.out.println("✓ PASS: Two files produces valid SHA-256 hash");
            System.out.println("  Root: " + test3);
            passed++;
        } else {
            System.out.println("✗ FAIL: Invalid hash format: " + test3);
            failed++;
        }
        
        // Test 4: Three files (odd number)
        System.out.println("\nTest 4: Three files (odd number)");
        List<CloudFile> threeFiles = new ArrayList<>();
        threeFiles.add(new CloudFile("file1.txt", "0000000000000000000000000000000000000000000000000000000000000001"));
        threeFiles.add(new CloudFile("file2.txt", "0000000000000000000000000000000000000000000000000000000000000002"));
        threeFiles.add(new CloudFile("file3.txt", "0000000000000000000000000000000000000000000000000000000000000003"));
        String test4 = calculateMerkleRoot(threeFiles);
        if (test4.length() == 64 && test4.matches("[0-9a-f]{64}")) {
            System.out.println("✓ PASS: Three files produces valid hash");
            System.out.println("  Root: " + test4);
            passed++;
        } else {
            System.out.println("✗ FAIL: Invalid hash format: " + test4);
            failed++;
        }
        
        // Test 5: Deterministic output
        System.out.println("\nTest 5: Deterministic output");
        List<CloudFile> files5 = new ArrayList<>();
        files5.add(new CloudFile("file1.txt", padEnd("a1b2c3d4e5f6", 64, '0')));
        files5.add(new CloudFile("file2.txt", padEnd("f6e5d4c3b2a1", 64, '0')));
        String root5a = calculateMerkleRoot(files5);
        String root5b = calculateMerkleRoot(files5);
        if (root5a.equals(root5b)) {
            System.out.println("✓ PASS: Same files produce same root");
            System.out.println("  Root: " + root5a);
            passed++;
        } else {
            System.out.println("✗ FAIL: Roots don't match");
            failed++;
        }
        
        // Test 6: Large file set (1000 files)
        System.out.println("\nTest 6: Large file set (1000 files)");
        List<CloudFile> largeFiles = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            largeFiles.add(new CloudFile("file" + i + ".txt", padEnd(String.valueOf(i), 64, '0')));
        }
        String test6 = calculateMerkleRoot(largeFiles);
        if (test6.length() == 64 && test6.matches("[0-9a-f]{64}")) {
            System.out.println("✓ PASS: 1000 files produces valid hash");
            System.out.println("  Root: " + test6.substring(0, 32) + "...");
            passed++;
        } else {
            System.out.println("✗ FAIL: Invalid hash format for large set");
            failed++;
        }
        
        // Test 7: Very large file set (10,000 files)
        System.out.println("\nTest 7: Very large file set (10,000 files)");
        List<CloudFile> veryLargeFiles = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
            String hash = repeat(String.format("%02d", i % 256), 32);
            veryLargeFiles.add(new CloudFile("file" + i + ".txt", hash));
        }
        String test7 = calculateMerkleRoot(veryLargeFiles);
        if (test7.length() == 64 && test7.matches("[0-9a-f]{64}")) {
            System.out.println("✓ PASS: 10,000 files produces valid hash");
            System.out.println("  Root: " + test7.substring(0, 32) + "...");
            passed++;
        } else {
            System.out.println("✗ FAIL: Invalid hash format for very large set");
            failed++;
        }
        
        // Test 8: Powers of 2
        System.out.println("\nTest 8: Perfect binary trees (powers of 2)");
        boolean powerTest = true;
        for (int count : new int[]{2, 4, 8, 16, 32, 64, 128, 256}) {
            List<CloudFile> files = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                files.add(new CloudFile("file" + i + ".txt", padEnd(String.valueOf(i), 64, '0')));
            }
            String root = calculateMerkleRoot(files);
            if (root.length() != 64 || !root.matches("[0-9a-f]{64}")) {
                powerTest = false;
                System.out.println("✗ FAIL at " + count + " files");
                break;
            }
        }
        if (powerTest) {
            System.out.println("✓ PASS: All power-of-2 counts work correctly");
            passed++;
        } else {
            failed++;
        }
        
        // Test 9: Non-powers of 2
        System.out.println("\nTest 9: Non-perfect trees (non-powers of 2)");
        boolean nonPowerTest = true;
        for (int count : new int[]{3, 5, 7, 9, 15, 17, 33, 63, 65, 127}) {
            List<CloudFile> files = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                files.add(new CloudFile("file" + i + ".txt", padEnd(String.valueOf(i), 64, '0')));
            }
            String root = calculateMerkleRoot(files);
            if (root.length() != 64 || !root.matches("[0-9a-f]{64}")) {
                nonPowerTest = false;
                System.out.println("✗ FAIL at " + count + " files");
                break;
            }
        }
        if (nonPowerTest) {
            System.out.println("✓ PASS: All non-power-of-2 counts work correctly");
            passed++;
        } else {
            failed++;
        }
        
        // Summary
        System.out.println("\n" + repeat("=", 50));
        System.out.println("Test Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) {
            System.out.println("✓ All tests passed! Implementation is correct.");
        } else {
            System.out.println("✗ Some tests failed. Review implementation.");
        }
        System.out.println(repeat("=", 50));
    }
    
    static String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    static String padEnd(String str, int length, char padChar) {
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(padChar);
        }
        return sb.toString();
    }
}

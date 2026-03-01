# Testing Guide

Comprehensive testing strategy for ObsidianBackup.

## Testing Pyramid

```
       ╱╲
      ╱ E2E╲
     ╱──────╲
    ╱   UI   ╲
   ╱──────────╲
  ╱Integration ╲
 ╱──────────────╲
╱  Unit Tests   ╲
────────────────────
```

## Unit Tests

Test individual components in isolation.

### ViewModel Tests

```kotlin
class BackupViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var viewModel: BackupViewModel
    private lateinit var mockRepository: BackupRepository
    
    @Before
    fun setup() {
        mockRepository = mock()
        viewModel = BackupViewModel(mockRepository)
    }
    
    @Test
    fun `createBackup success updates ui state`() = runTest {
        val backup = createTestBackup()
        whenever(mockRepository.createBackup(any())).thenReturn(Result.Success(backup))
        
        viewModel.createBackup(listOf("com.example.app"))
        
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(backup, state.data)
    }
}
```

### Use Case Tests

```kotlin
class CreateBackupUseCaseTest {
    @Test
    fun `execute creates backup successfully`() = runTest {
        val repository = FakeBackupRepository()
        val useCase = CreateBackupUseCase(repository)
        
        val result = useCase.execute(listOf("com.example.app"))
        
        assertTrue(result is Result.Success)
        assertEquals(1, repository.backups.size)
    }
}
```

### Repository Tests

```kotlin
class BackupRepositoryTest {
    private lateinit var repository: BackupRepositoryImpl
    private lateinit var database: BackupDatabase
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            context,
            BackupDatabase::class.java
        ).build()
        repository = BackupRepositoryImpl(database.backupDao())
    }
    
    @Test
    fun `getBackups returns all backups`() = runTest {
        val backup1 = createTestBackup(id = "1")
        val backup2 = createTestBackup(id = "2")
        repository.insertBackup(backup1)
        repository.insertBackup(backup2)
        
        val backups = repository.getBackups().first()
        
        assertEquals(2, backups.size)
    }
}
```

## Integration Tests

Test component interactions.

### Database Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class BackupDaoTest {
    private lateinit var database: BackupDatabase
    private lateinit var dao: BackupDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BackupDatabase::class.java
        ).build()
        dao = database.backupDao()
    }
    
    @Test
    fun insertAndRetrieveBackup() = runBlocking {
        val backup = createTestBackupEntity()
        dao.insert(backup)
        
        val retrieved = dao.getById(backup.id)
        
        assertEquals(backup, retrieved)
    }
}
```

## UI Tests

Test user interface with Espresso and Compose Testing.

### Compose UI Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun dashboard_displays_backup_count() {
        val backups = listOf(
            createTestBackup(),
            createTestBackup()
        )
        
        composeTestRule.setContent {
            DashboardScreen(backups = backups)
        }
        
        composeTestRule
            .onNodeWithText("2 Backups")
            .assertIsDisplayed()
    }
    
    @Test
    fun clicking_backup_button_triggers_backup() {
        var clicked = false
        
        composeTestRule.setContent {
            DashboardScreen(
                onBackupClick = { clicked = true }
            )
        }
        
        composeTestRule
            .onNodeWithText("Backup Now")
            .performClick()
        
        assertTrue(clicked)
    }
}
```

## Test Utilities

### Fakes

```kotlin
class FakeBackupRepository : BackupRepository {
    val backups = mutableListOf<Backup>()
    
    override suspend fun createBackup(apps: List<String>): Result<Backup> {
        val backup = Backup(
            id = UUID.randomUUID().toString(),
            apps = apps
        )
        backups.add(backup)
        return Result.Success(backup)
    }
}
```

### Test Data Builders

```kotlin
fun createTestBackup(
    id: String = "test-id",
    name: String = "Test Backup",
    timestamp: Long = System.currentTimeMillis()
): Backup {
    return Backup(
        id = id,
        name = name,
        timestamp = timestamp
    )
}
```

## Running Tests

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew connectedAndroidTest

# All tests
./gradlew check

# Specific test
./gradlew test --tests BackupViewModelTest
```

## Best Practices

1. **Follow AAA Pattern**: Arrange, Act, Assert
2. **One Assertion Per Test**: Test one thing
3. **Use Descriptive Names**: Clearly state what is tested
4. **Test Edge Cases**: Test boundary conditions
5. **Mock External Dependencies**: Isolate tests
6. **Fast Tests**: Keep tests fast
7. **Independent Tests**: Tests shouldn't depend on each other
8. **Clean Up**: Clean up after tests

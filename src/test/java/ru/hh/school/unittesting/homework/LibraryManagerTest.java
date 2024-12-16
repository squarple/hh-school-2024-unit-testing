package ru.hh.school.unittesting.homework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {
  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @Test
  void addBookAddNewBook() {
    libraryManager.addBook("War and Peace", 1);
    assertEquals(1, libraryManager.getAvailableCopies("War and Peace"));
  }

  @Test
  void addBookAddSameBook() {
    libraryManager.addBook("War and Peace", 1);
    libraryManager.addBook("War and Peace", 5);
    assertEquals(6, libraryManager.getAvailableCopies("War and Peace"));
  }

  @Test
  void borrowBookSuccess() {
    when(userService.isUserActive("Ivan Ivanov")).thenReturn(true);

    libraryManager.addBook("War and Peace", 5);

    boolean isBorrowSuccess = libraryManager.borrowBook("War and Peace", "Ivan Ivanov");
    assertTrue(isBorrowSuccess);

    int availableCopies = libraryManager.getAvailableCopies("War and Peace");
    assertEquals(4, availableCopies);

    verify(notificationService, times(1)).notifyUser("Ivan Ivanov", "You have borrowed the book: War and Peace");
  }

  @Test
  void borrowBookNotActiveUser() {
    when(userService.isUserActive("Ivan Ivanov")).thenReturn(false);

    libraryManager.addBook("War and Peace", 5);
    boolean isBorrowSuccess = libraryManager.borrowBook("War and Peace", "Ivan Ivanov");
    assertFalse(isBorrowSuccess);

    int availableCopies = libraryManager.getAvailableCopies("War and Peace");
    assertEquals(5, availableCopies);

    verify(notificationService, times(1)).notifyUser("Ivan Ivanov", "Your account is not active.");
  }

  @Test
  void borrowBookNoAviablesCopies() {
    when(userService.isUserActive("Ivan Ivanov")).thenReturn(true);

    libraryManager.addBook("War and Peace", 0);

    boolean isBorrowSuccess = libraryManager.borrowBook("War and Peace", "Ivan Ivanov");
    assertFalse(isBorrowSuccess);

    int availableCopies = libraryManager.getAvailableCopies("War and Peace");
    assertEquals(0, availableCopies);

    verify(notificationService, times(0)).notifyUser(any(), any());
  }

  @Test
  void returnBookNoSuchBook() {
    boolean isReturn = libraryManager.returnBook("War and Peace", "Ivan Ivanov");
    assertFalse(isReturn);
  }

  @Test
  void returnBookWrongBook() {
    libraryManager.addBook("War and Peace", 1);

    boolean isReturn = libraryManager.returnBook("Anna Karenina", "Ivan Ivanov");
    assertFalse(isReturn);

    verify(notificationService, times(0)).notifyUser(any(), any());
  }

  @Test
  void returnBookWrongUser() {
    when(userService.isUserActive("Ivan Ivanov")).thenReturn(true);

    libraryManager.addBook("War and Peace", 1);

    libraryManager.borrowBook("War and Peace", "Ivan Ivanov");

    boolean isReturn = libraryManager.returnBook("War and Peace", "John Doe");
    assertFalse(isReturn);

    verify(notificationService, times(1)).notifyUser("Ivan Ivanov", "You have borrowed the book: War and Peace");
    verifyNoMoreInteractions(notificationService);
  }

  @Test
  void returnBookSuccess() {
    when(userService.isUserActive("Ivan Ivanov")).thenReturn(true);

    libraryManager.addBook("War and Peace", 1);

    libraryManager.borrowBook("War and Peace", "Ivan Ivanov");

    boolean isReturn = libraryManager.returnBook("War and Peace", "Ivan Ivanov");
    assertTrue(isReturn);

    verify(notificationService, times(1)).notifyUser("Ivan Ivanov", "You have returned the book: War and Peace");
  }

  @Test
  void getAvailableCopiesNoSuchBook() {
    int availableCopies = libraryManager.getAvailableCopies("War and Peace");
    assertEquals(0, availableCopies);
  }

  @Test
  void getAvailableCopiesAfterBorrowAndAfterReturn() {
    when(userService.isUserActive("Ivan Ivanov")).thenReturn(true);
    libraryManager.addBook("War and Peace", 1);
    assertEquals(1, libraryManager.getAvailableCopies("War and Peace"));

    libraryManager.borrowBook("War and Peace", "Ivan Ivanov");
    assertEquals(0, libraryManager.getAvailableCopies("War and Peace"));

    libraryManager.returnBook("War and Peace", "Ivan Ivanov");
    assertEquals(1, libraryManager.getAvailableCopies("War and Peace"));
  }

  @Test
  void getAvailableCopiesHaveCopies() {
    libraryManager.addBook("War and Peace", 5);

    int availableCopies = libraryManager.getAvailableCopies("War and Peace");
    assertEquals(5, availableCopies);
  }

  @Test
  void getAvailableCopiesBorrowAllCopies() {
    when(userService.isUserActive("Ivan Ivanov")).thenReturn(true);

    libraryManager.addBook("War and Peace", 5);

    libraryManager.borrowBook("War and Peace", "Ivan Ivanov");
    libraryManager.borrowBook("War and Peace", "Ivan Ivanov");
    libraryManager.borrowBook("War and Peace", "Ivan Ivanov");
    libraryManager.borrowBook("War and Peace", "Ivan Ivanov");
    libraryManager.borrowBook("War and Peace", "Ivan Ivanov");

    int availableCopies = libraryManager.getAvailableCopies("War and Peace");
    assertEquals(0, availableCopies);
  }

  @Test
  void calculateDynamicLateFeeShouldThrowExceptionIfNegativeOverdueDays() {
    Exception ex = Assertions.assertThrowsExactly(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, false, false));
    assertEquals("Overdue days cannot be negative.", ex.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
      "0, false,  false,  0.0",
      "0, true,   true,   0.0",
      "0, false,  true,   0.0",
      "0, true,   false,  0.0",

      "5,  false, false,  2.5",
      "10, true,  false,  7.5",
      "13, false, true,   5.2",
      "27, true,  true,   16.2",
  })
  void calculateDynamicLateFeeTest(
      int overdueDays,
      boolean isBestseller,
      boolean isPremiumMember,
      double expectedFee) {
    double actualFee = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);
    assertEquals(expectedFee, actualFee, "The fee should be " + expectedFee + " but was " + actualFee);
  }
}

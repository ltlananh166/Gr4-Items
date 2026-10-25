package view;

public class MainView {
    private final ConsoleInput input;

    public MainView(ConsoleInput input) {
        this.input = input;
    }

    public void display() {
        boolean isRunning = true;
        while (isRunning) {
            System.out.println("\n================ FLASH SALE SYSTEM =================");
            System.out.println("1. Guest access (Browse products)");
            System.out.println("2. Login / Register (Customer)");
            System.out.println("3. Admin Login");
            System.out.println("0. Exit");
            System.out.println("=======================================================");

            int choice = input.readIntMinMax("Please choose an option (0->3): ", 0, 3, "Please enter a number between 0 and 3!");

            switch (choice) {
                case 1:
                    System.out.println("--> Opening product browsing screen...");
                    //gọi flashSaleShoppingView.browse();
                    break;
                case 2:
                    System.out.println("--> Opening account screen...");
                    //gọi customerAccountView.display();
                    break;
                case 3:
                    System.out.println("--> Opening Admin screen...");
                    //gọi adminView.display();
                    break;
                case 0:
                    System.out.println("Thank you for using the system! Goodbye!");
                    isRunning = false;
                    break;
            }
        }
    }
}
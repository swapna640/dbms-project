import ui.PizzaUI;
import java.util.InputMismatchException;
import java.util.Scanner;
public class Main {
    public static void main(String args[]) {
        System.out.println(" ___________________________________________________________________________________________________________________________\n" +
                "\n" +
                "|                                                                                                                           |\n" +
                "|  ██████╗ ██╗███████╗███████╗█████╗     ███████╗█████╗ ██╗     ███████╗███████╗    ██████╗  ██████╗                        |\n" +
                "|  ██╔══██╗██║╚══███╔╝╚══███╔╝██╔══██╗    ██╔════╝██╔══██╗██║     ██╔════╝██╔════╝    ██╔══██╗ ██╔══██╗                       |\n" +
                "|  ██████╔╝██║  ███╔╝   ███╔╝ ███████║    ███████╗███████║██║     █████╗  ███████╗    ██║  ██║ ██████╔╝                       |\n" +
                "|  ██╔═══╝ ██║ ███╔╝   ███╔╝  ██╔══██║    ╚════██║██╔══██║██║     ██╔══╝  ╚════██║    ██║  ██║ ██╔══██╗                       |\n" +
                "|  ██║     ██║███████╗███████╗██║  ██║    ███████║██║  ██║███████╗███████╗███████║    ██████╔╝ ██████╔╝  ██╗                  |\n" +
                "|  ╚═╝     ╚═╝╚══════╝╚══════╝╚═╝  ╚═╝    ╚══════╝╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝    ╚═════╝  ╚═════╝   ╚═╝                  |\n" +
                "|___________________________________________________________________________________________________________________________|\n");
        Scanner sc = new Scanner(System.in);
        int f = 1;
        while(f != 0)
        {
            System.out.println("1 to view schema \n2 to view available SQL queries\n3 to view available PL/SQL queries\n4 to view relational algebra and relational calculus statements");
            char ch = sc.next().charAt(0);
            switch(ch)
            {
                case '1':
                    System.out.println("Schema:");
                    //call schema class
                    break;
                case '2':
                    System.out.println("SQL queries:");
                    PizzaUI.menu();
                    break;
                case '3':
                    System.out.println("PL/SQL queries:");
                    //call pl/sql queries
                    break;
                case '4':
                    System.out.println("Relational algebra and relational calculus statements:");
                    //call relational algebra and calculus statements
                    break;
                default:
                    System.out.println("Sorry, the value you have entered seems to be inappropriate.\nPress 1 to try again, and 2 to exit");
                    try
                    {
                        int terminateChoice = sc.nextInt();
                        if(terminateChoice == 1)
                            f = 1;
                        else if(terminateChoice == 2)
                            f = 0;
                        else
                            System.out.println("That's not a choice!");
                    }
                    catch(InputMismatchException e)
                    {
                        System.out.println("That wasn't an integer! Please try again");
                        f = 1;
                    }
            }
            int continueChoice;
            System.out.println("Do you want to continue? Press 0 if not, otherwise press any other number");
            if(sc.hasNextInt())
            {
                continueChoice = sc.nextInt();
                if(continueChoice == 0)
                    f = 0;
            }
        }
        System.out.println("You are now exiting the application...");
    }
}
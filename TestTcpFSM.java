import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TestTcpFSM {
    private static final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static final PrintStream originalOut = System.out;

    private static void setupStreams() {
        System.setOut(new PrintStream(outContent));
    }

    private static void restoreStreams() {
        System.setOut(originalOut);
    }

    private static void runTest(String input, String testName, String description) {

        restoreStreams();

        System.out.println("\n=== Test Case: " + testName + " ===");
        System.out.println("Description: " + description);


        outContent.reset();

        ByteArrayInputStream testIn = new ByteArrayInputStream(input.getBytes());
        System.setIn(testIn);
        setupStreams();

        TcpFSM.main(new String[]{});

        restoreStreams();

        System.out.println("\nInput Events:");
        for (String event : input.trim().split("\n")) {
            System.out.println("  " + event + " - " + getEventDescription(event));
        }

        System.out.println("\nOutput:");
        System.out.println(outContent.toString());
        System.out.println("--------------------------------");
    }

    private static String getEventDescription(String event) {
        switch (event) {
            case "PASSIVE": return "Passive Open";
            case "ACTIVE": return "Active Open";
            case "SYN": return "SYN received";
            case "SYNACK": return "SYN + ACK received";
            case "ACK": return "ACK received";
            case "RDATA": return "Data received from network";
            case "SDATA": return "Data to be sent from application";
            case "FIN": return "FIN received";
            case "CLOSE": return "Client or Server issues close()";
            case "TIMEOUT": return "Timed wait ends";
            default: return "Unknown event";
        }
    }


    public static void main(String[] args) {
        setupStreams();

        try {
            String test1 = String.join("\n",
                    "PASSIVE",
                    "SYN",
                    "ACK",
                    "RDATA",
                    "SDATA",
                    "CLOSE\n"
            );
            runTest(test1, "Server Passive Open",
                    "Tests server-side connection establishment and data transfer");

            String test2 = String.join("\n",
                    "ACTIVE",
                    "SYNACK",
                    "RDATA",
                    "SDATA",
                    "CLOSE\n"
            );
            runTest(test2, "Client Active Open",
                    "Tests client-side connection establishment and data transfer");

            String test3 = String.join("\n",
                    "PASSIVE",
                    "SYN",
                    "ACK",
                    "RDATA",
                    "RDATA",
                    "SDATA",
                    "SDATA",
                    "CLOSE\n"
            );
            runTest(test3, "Multiple Data Transfers",
                    "Tests multiple data transfers in established connection");

            String test4 = String.join("\n",
                    "PASSIVE",
                    "SYN",
                    "ACK",
                    "FIN",
                    "CLOSE",
                    "ACK",
                    "TIMEOUT\n"
            );
            runTest(test4, "Normal Close Sequence",
                    "Tests normal connection termination sequence");

            String test5 = String.join("\n",
                    "PASSIVE",
                    "SYN",
                    "ACK",
                    "RDATA",
                    "FIN",
                    "RDATA",
                    "SDATA\n"
            );
            runTest(test5, "Invalid Event Handling",
                    "Tests handling of invalid events in different states");

        } finally {
            restoreStreams();
        }
    }
}
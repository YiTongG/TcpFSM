import Fsm.*;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

class TcpEvent extends Event {
    public TcpEvent(String name) {
        super(name);
    }
}

class DataAction extends Action {
    private final EstablishedState state;
    private final boolean isReceive;

    public DataAction(EstablishedState state, boolean isReceive) {
        this.state = state;
        this.isReceive = isReceive;
    }

    @Override
    public void execute(FSM fsm, Event event) {
        if (isReceive) {
            int count = state.incrementDataReceived();
            System.out.println("DATA received " + count);
        } else {
            int count = state.incrementDataSent();
            System.out.println("DATA sent " + count);
        }
    }
}

class NoOpAction extends Action {
    @Override
    public void execute(FSM fsm, Event event) {
        System.out.println("Event " + event.getName() + " received, current State is " + fsm.currentState().getName());
    }
}
class EstablishedState extends State {
    private int dataReceived = 0;
    private int dataSent = 0;

    public EstablishedState() {
        super("ESTABLISHED");
    }

    public int incrementDataReceived() {
        return ++dataReceived;
    }

    public int incrementDataSent() {
        return ++dataSent;
    }
}

public class TcpFSM {
    private static FSM fsm;
    private static Map<String, Event> eventMap = new HashMap<>();

    private static void setupFSM() throws FsmException {
        State closed = new State("CLOSED") {};
        State listen = new State("LISTEN") {};
        State synSent = new State("SYN_SENT") {};
        State synRcvd = new State("SYN_RCVD") {};
        EstablishedState established = new EstablishedState();
        State finWait1 = new State("FIN_WAIT_1") {};
        State finWait2 = new State("FIN_WAIT_2") {};
        State closing = new State("CLOSING") {};
        State timeWait = new State("TIME_WAIT") {};
        State closeWait = new State("CLOSE_WAIT") {};
        State lastAck = new State("LAST_ACK") {};

        eventMap.put("PASSIVE", new TcpEvent("PASSIVE"));
        eventMap.put("ACTIVE", new TcpEvent("ACTIVE"));
        eventMap.put("SYN", new TcpEvent("SYN"));
        eventMap.put("SYNACK", new TcpEvent("SYNACK"));
        eventMap.put("ACK", new TcpEvent("ACK"));
        eventMap.put("RDATA", new TcpEvent("RDATA"));
        eventMap.put("SDATA", new TcpEvent("SDATA"));
        eventMap.put("FIN", new TcpEvent("FIN"));
        eventMap.put("CLOSE", new TcpEvent("CLOSE"));
        eventMap.put("TIMEOUT", new TcpEvent("TIMEOUT"));

        fsm = new FSM("TCP", closed);

        Action noOp = new NoOpAction();
        Action receiveData = new DataAction(established, true);
        Action sendData = new DataAction(established, false);

        fsm.addTransition(new Transition(closed, eventMap.get("PASSIVE"), listen, noOp));
        fsm.addTransition(new Transition(closed, eventMap.get("ACTIVE"), synSent, noOp));
        fsm.addTransition(new Transition(listen, eventMap.get("SYN"), synRcvd, noOp));
        fsm.addTransition(new Transition(listen, eventMap.get("CLOSE"), closed, noOp));
        fsm.addTransition(new Transition(synSent, eventMap.get("SYNACK"), established, noOp));
        fsm.addTransition(new Transition(synSent, eventMap.get("CLOSE"), closed, noOp));
        fsm.addTransition(new Transition(synRcvd, eventMap.get("ACK"), established, noOp));
        fsm.addTransition(new Transition(synRcvd, eventMap.get("CLOSE"), finWait1, noOp));

        fsm.addTransition(new Transition(established, eventMap.get("RDATA"), established, receiveData));
        fsm.addTransition(new Transition(established, eventMap.get("SDATA"), established, sendData));
        fsm.addTransition(new Transition(established, eventMap.get("CLOSE"), finWait1, noOp));
        fsm.addTransition(new Transition(established, eventMap.get("FIN"), closeWait, noOp));

        fsm.addTransition(new Transition(finWait1, eventMap.get("FIN"), closing, noOp));
        fsm.addTransition(new Transition(finWait1, eventMap.get("ACK"), finWait2, noOp));
        fsm.addTransition(new Transition(finWait2, eventMap.get("FIN"), timeWait, noOp));
        fsm.addTransition(new Transition(closing, eventMap.get("ACK"), timeWait, noOp));
        fsm.addTransition(new Transition(timeWait, eventMap.get("TIMEOUT"), closed, noOp));
        fsm.addTransition(new Transition(closeWait, eventMap.get("CLOSE"), lastAck, noOp));
        fsm.addTransition(new Transition(lastAck, eventMap.get("ACK"), closed, noOp));

        //fsm.traceOn();
    }

    // isValidTransition
    private static boolean isValidTransition(String currentState, String event) {
        Map<String, Set<String>> validTransitions = new HashMap<>();

        validTransitions.put("CLOSED", new HashSet<>(Arrays.asList("PASSIVE", "ACTIVE")));
        validTransitions.put("LISTEN", new HashSet<>(Arrays.asList("SYN", "CLOSE")));
        validTransitions.put("SYN_SENT", new HashSet<>(Arrays.asList("SYNACK", "CLOSE")));
        validTransitions.put("SYN_RCVD", new HashSet<>(Arrays.asList("ACK", "CLOSE")));
        validTransitions.put("ESTABLISHED", new HashSet<>(Arrays.asList("CLOSE", "FIN", "RDATA", "SDATA")));
        validTransitions.put("FIN_WAIT_1", new HashSet<>(Arrays.asList("FIN", "ACK")));
        validTransitions.put("FIN_WAIT_2", new HashSet<>(Arrays.asList("FIN")));
        validTransitions.put("CLOSING", new HashSet<>(Arrays.asList("ACK")));
        validTransitions.put("TIME_WAIT", new HashSet<>(Arrays.asList("TIMEOUT")));
        validTransitions.put("CLOSE_WAIT", new HashSet<>(Arrays.asList("CLOSE")));
        validTransitions.put("LAST_ACK", new HashSet<>(Arrays.asList("ACK")));

        Set<String> validEvents = validTransitions.get(currentState);
        return validEvents != null && validEvents.contains(event);
    }

    public static void main(String[] args) {
        try {
            setupFSM();
            Scanner scanner = new Scanner(System.in);

            while (scanner.hasNext()) {
                String eventString = scanner.next().trim();

                Event event = eventMap.get(eventString);
                if (event == null) {
                    System.out.println("Error: unexpected Event: " + eventString);
                    continue;
                }

                try {
                    fsm.doEvent(event);
                } catch (FsmException e) {
                    System.out.println(e.toString());
                }
            }

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
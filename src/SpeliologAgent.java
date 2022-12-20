import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.ControllerException;


public class SpeliologAgent extends Agent {

    private AID environmentAgent = null;
    private AID navigatorAgent = null;

    static final String BREEZE = "Breeze";
    static final String STENCH = "Stench";
    static final String GLITTER = "Glitter";
    static final String BUMP = "Bump";
    static final String SCREAM = "Scream";

    @Override
    protected void setup() {
        super.setup();

        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        DFAgentDescription templateNavigator = new DFAgentDescription();
        ServiceDescription sdNavigator = new ServiceDescription();
        sd.setType("environment-agent");
        sdNavigator.setType("navigator");
        templateNavigator.addServices(sdNavigator);
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            environmentAgent = result[0].getName();
            DFAgentDescription[] result1 = DFService.search(this, templateNavigator);
            navigatorAgent = result1[0].getName();
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new SpeliologBehaviour() {
        });
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        System.out.println("Player-agent " + getAID().getName() + " terminating.");
    }

    private class SpeliologBehaviour extends Behaviour {

        private Integer step = 0;
        private MessageTemplate mt = null;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    ACLMessage requestMessage = new ACLMessage(ACLMessage.REQUEST);
                    requestMessage.setConversationId("request-to-envinromnent-id");
                    requestMessage.addReceiver(environmentAgent);
                    requestMessage.setReplyWith("request" + System.currentTimeMillis());
                    myAgent.send(requestMessage);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("request-to-envinromnent-id"),
                            MessageTemplate.MatchInReplyTo(requestMessage.getReplyWith()));
                    step++;
                }
                case 1 -> {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            try {
                                WumpusPercept percept = (WumpusPercept) reply.getContentObject();
                                System.out.println("Envinroment is " + percept);
                                String messageContent = createStringToNavigator(percept);
                                ACLMessage requestMessage = new ACLMessage(ACLMessage.CFP);
                                requestMessage.setConversationId("navigator-message-id");
                                requestMessage.addReceiver(navigatorAgent);
                                requestMessage.setContent(messageContent);
                                requestMessage.setReplyWith("response" + System.currentTimeMillis());
                                myAgent.send(requestMessage);
                                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("navigator-message-id"),
                                        MessageTemplate.MatchInReplyTo(requestMessage.getReplyWith()));
                                step++;
                            } catch (UnreadableException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        block();
                    }
                }
                case 2 -> {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CFP) {
                            ACLMessage requestMessage = new ACLMessage(ACLMessage.CFP);
                            requestMessage.setConversationId("navigator-action-id");
                            requestMessage.addReceiver(environmentAgent);
                            requestMessage.setContent(reply.getContent());
                            requestMessage.setReplyWith("response" + System.currentTimeMillis());
                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("navigator-action-id"),
                                    MessageTemplate.MatchInReplyTo(requestMessage.getReplyWith()));
                            myAgent.send(requestMessage);
                            step++;
                        }
                    } else {
                        block();
                    }
                }
                case 3 -> {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                            ActionResult result = ActionResult.valueOf(reply.getContent());
                            switch (result) {
                                case VICTORY, DEFEAT -> {
                                    System.out.println(result);
                                    try {
                                        getContainerController().getPlatformController().kill();
                                    } catch (ControllerException e) {
                                        throw new RuntimeException(e);
                                    }
//                                    doDelete();
                                }
                                case NOTHING -> step = 0;
                            }
                        }
                    } else {
                        block();
                    }
                }
            }
        }

        @Override
        public boolean done() {
            return step == 4;
        }
    }

    private String createStringToNavigator(WumpusPercept percept) {
        StringBuilder sb = new StringBuilder();
        if (percept.isScream()) {
            sb.append(SCREAM);
            sb.append(".");
        }
        if (percept.isBreeze()) {
            sb.append(BREEZE);
            sb.append(".");
        }
        if (percept.isBump()) {
            sb.append(BUMP);
            sb.append(".");
        }
        if (percept.isGlitter()) {
            sb.append(GLITTER);
            sb.append(".");
        }
        if (percept.isStench()) {
            sb.append(STENCH);
            sb.append(".");
        }
        return sb.toString();
    }
}

package cz.agents.madla.creator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import cz.agents.alite.communication.PerformerCommunicator;
import cz.agents.alite.communication.QueuedCommunicator;
import cz.agents.alite.communication.channel.CommunicationChannelException;
import cz.agents.alite.communication.channel.DirectCommunicationChannel.DefaultReceiverTable;
import cz.agents.alite.communication.channel.DirectCommunicationChannel.ReceiverTable;
import cz.agents.alite.communication.channel.DirectCommunicationChannelAsync;
import cz.agents.alite.creator.Creator;
import cz.agents.dimaptools.DIMAPWorldInterface;
import cz.agents.dimaptools.DefaultDIMAPWorld;
import cz.agents.dimaptools.communication.protocol.DefaultEncoder;
import cz.agents.dimaptools.experiment.DataAccumulator;
import cz.agents.dimaptools.experiment.Trace;
import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.input.sas.SASParser;
import cz.agents.dimaptools.input.sas.SASPreprocessor;
import cz.agents.madla.executor.IPCOutputExecutor;
import cz.agents.madla.planner.Planner;

public class MAPDDLCreator implements Creator {

    private final static Logger LOGGER = Logger.getLogger(MAPDDLCreator.class);


    private static final String TRANSLATOR = "./misc/fd/src/translate/translate.py";
    private static final String PREPROCESSOR = "./preprocess-runner";
    private static final String CONVERTOR = "./misc/ma-pddl/ma-to-pddl.py";
    private static final String RESOURCES = "./";
    private static final String TEMP = "./temp";
    private static final String OUTPUT = "./out.csv";
    
    private final ReceiverTable receiverTable = new DefaultReceiverTable();
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final Set<Planner> agentSet = new LinkedHashSet<Planner>();
    private final Set<Thread> threadSet = new LinkedHashSet<Thread>();

    private boolean fromSAS = true;

    private String domainFileName;
    private String problemFileName;
    private String sasFileName;
    private String agentFileName;

    private String heuristic;
    private int recursionLevel;
    private int timeLimitMin;

    private boolean sasSolvable = false;


    protected SASPreprocessor preprocessor;


    @Override
    public void init(String[] args) {

        for(int i=0;i<args.length;i++){
            System.out.println(args[i]);
        }

        if (args.length != 6 && args.length != 7) {
            System.out.println("provided args: " + Arrays.toString(args));
            System.out.println("Usage (from SAS): <output>.sas <agents>.addl <add|max|FF> <recursionLevel> <time limit (min)>\nUsage (from PDDL): <domain>.pddl <problem>.pddl <agents>.addl <add|max|FF> <recursionLevel> <time limit (min)>");
            System.exit(1);
        }

        if (args.length == 6){
            sasFileName = args[1];
            agentFileName = args[2];
            heuristic = args[3];
            recursionLevel = Integer.parseInt(args[4]);
            timeLimitMin = Integer.parseInt(args[5]);
        }

        if (args.length == 7){
            fromSAS = false;
            sasFileName = "output";
            domainFileName = args[1];
            problemFileName = args[2];
            agentFileName = args[3];
            heuristic = args[4];
            recursionLevel = Integer.parseInt(args[5]);
            timeLimitMin = Integer.parseInt(args[6]);
        }

        Trace.setFileStream("Log/trace.log");
    }



    @Override
    public void create() {
        long startTime = System.currentTimeMillis();
        
        DataAccumulator.startNewAccumulator(domainFileName, problemFileName, 0, heuristic, recursionLevel);
        DataAccumulator.getAccumulator().startTimeMs = startTime;
        DataAccumulator.getAccumulator().setOutputFile(OUTPUT);
        
        runConvert();

        LOGGER.info(">>> CREATION");
        LOGGER.info(">>>   sas: " + sasFileName);
        LOGGER.info(">>>   agents: " + agentFileName);
        
        File agentFile = new File(RESOURCES + agentFileName);
        if (!agentFile.exists()) {
            LOGGER.fatal("Agent file " + RESOURCES + agentFileName + " does not exist!");
            System.exit(1);
        }
        ADDLObject addl = new ADDLParser().parse(agentFile);
        
        DataAccumulator.getAccumulator().agents = addl.getAgentCount();
        
        if(domainFileName == null) domainFileName = sasFileName;
        if(problemFileName == null) problemFileName = sasFileName;

        if(!fromSAS){
        	
//                Timer.setTranslationTime(System.nanoTime());
            runTranslate();
//                Timer.setTranslationTime(System.nanoTime()-Timer.getTranslationTime());
//                Timer.setPreprocessingTime(System.nanoTime());
                        
            if (!sasSolvable) {
            	 System.out.println("Plan not found!");
                 DataAccumulator.getAccumulator().finishTimeMs = System.currentTimeMillis();
                 DataAccumulator.getAccumulator().finished = false;
                 DataAccumulator.getAccumulator().planLength = -1;
                 DataAccumulator.getAccumulator().planValid = false; 
                 DataAccumulator.getAccumulator().writeOutput(Planner.FORCE_EXIT_AFTER_WRITE);
            }
            
            
            runPreprocess();
//                Timer.setPreprocessingTime(System.nanoTime()-Timer.getPreprocessingTime());
        }

        File sasFile = new File(sasFileName);
        if (!sasFile.exists()) {
            LOGGER.fatal("SAS file " + sasFileName + " does not exist!");
            System.exit(1);
        }

        SASParser parser = new SASParser(sasFile);
        preprocessor = new SASPreprocessor(parser.getDomain(), addl);

        DataAccumulator.getAccumulator().startAfterPreprocessTimeMs = System.currentTimeMillis();

//            Timer.setCreatEntetiesTime(System.nanoTime());
        createEntities(addl);
//            Timer.setCreatEntetiesTime(System.nanoTime()-Timer.getCreatEntetiesTime());
//            Timer.setRunEntetiesTime(System.nanoTime());
        runEntities();
//            Timer.setRunEntetiesTime(System.nanoTime()-Timer.getRunEntetiesTime());

//        writeOutput();

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOGGER.warn("Shutdown interrupted!");
        }
    }


    
    private void runConvert(){
    	String path = domainFileName.substring(0, domainFileName.lastIndexOf("/"));
    	String domain = domainFileName.substring(domainFileName.lastIndexOf("/")+1, domainFileName.indexOf("."));
    	String problem = problemFileName.substring(problemFileName.lastIndexOf("/")+1, problemFileName.indexOf("."));
    	
        try {
            String cmd = CONVERTOR + " " + RESOURCES+path + " " + domain + " " + problem + " " + TEMP;
            LOGGER.info("RUN: " + cmd);
            Process pr = Runtime.getRuntime().exec(cmd);

            pr.waitFor();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        domainFileName = TEMP + "/" + domain + ".pddl";
        problemFileName = TEMP + "/" + problem + ".pddl";
    }



    private void runTranslate(){
        try {
            String cmd = TRANSLATOR + " " + domainFileName + " " + problemFileName;
            LOGGER.info("RUN: " + cmd);
            Process pr = Runtime.getRuntime().exec(cmd);

            pr.waitFor();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
                
        try (FileReader fr = new FileReader("output.sas")) {
        
            File f = new File("output.sas");

        	BufferedReader br = new BufferedReader(fr);
        	
            String line;
            
            if ((line = br.readLine()) != null) {
            	
                LOGGER.debug("test: " + f.getAbsolutePath());
            	
               if (line.equals("unsolvable"))            
                   sasSolvable = false;
               else
                   sasSolvable = true;
            }
            
        } catch (FileNotFoundException e) {
        	 LOGGER.fatal("SAS file " + "output.sas" + " does not exist!");
             System.exit(1);
		} catch (IOException e) {
			 LOGGER.fatal("SAS file " + "output.sas" + " bad!");
	            System.exit(1);
		}
        
    }

    private void runPreprocess() {
        try {
            String cmd = PREPROCESSOR;
            LOGGER.info("RUN: " + cmd);
            Process pr = Runtime.getRuntime().exec(cmd);
            pr.waitFor();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void createEntities(ADDLObject addl) {
        LOGGER.info(">>> ENTITIES CREATION");

        IPCOutputExecutor executor = new IPCOutputExecutor();

        for (String agentName: addl.getAgentList()) {

            DIMAPWorldInterface world = initWorld(agentName,addl.getAgentCount());

            agentSet.add(new Planner(heuristic,recursionLevel,world,executor,(long)timeLimitMin*60L*1000L));

            executor.addProblem(world.getProblem());
        }

        executor.setInitAndGoal(preprocessor.getGlobalInit(), preprocessor.getGlobalGoal());

    }


    public PerformerCommunicator initQueuedCommunicator(String address){
        QueuedCommunicator communicator = new QueuedCommunicator(address);
        try {

            communicator.handleMessageClass(Object.class);

            communicator.addChannel(new DirectCommunicationChannelAsync(communicator, receiverTable, executorService));
        } catch (CommunicationChannelException e) {
            LOGGER.fatal("Communication channel creation error!", e);
            System.exit(1);
        }

        return communicator;
    }

    public DIMAPWorldInterface initWorld(String agentName, int totalAgents){

        return new DefaultDIMAPWorld(
                agentName,
                initQueuedCommunicator(agentName),
                new DefaultEncoder(),
                preprocessor.getProblemForAgent(agentName),
                totalAgents
                );
    }


    @SuppressWarnings("deprecation")
    private void runEntities() {
        LOGGER.info(">>> ENTITIES RUNNING");


        final Thread mainThread = Thread.currentThread();
        final UncaughtExceptionHandler exceptionHandler = new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOGGER.error("Uncaught exception in agent/planner thread!", e);

                DataAccumulator.getAccumulator().finishTimeMs = DataAccumulator.getAccumulator().startTimeMs-1;
                DataAccumulator.getAccumulator().writeOutput(Planner.FORCE_EXIT_AFTER_WRITE);

                for (final Planner agent : agentSet) {
                    agent.getWorld().getCommPerformer().performClose();
                }

                mainThread.interrupt();
            }

        };

        for (final Planner agent : agentSet) {

//            final Communicator comm = initCommunicator(agent);

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    agent.planAndExecuteFinal();
                }
            }, agent.getName());
            thread.setUncaughtExceptionHandler(exceptionHandler);
            threadSet.add(thread);
            thread.start();
        }

        try {
            for (Thread thread : threadSet) {
                thread.join();
            }
        } catch (InterruptedException e) {
            LOGGER.debug("Main thread interrupted.");
        }

        for (Thread thread : threadSet) {
            if (thread.isAlive()) {
                // TODO: refactor using a stop flag
                thread.stop();
            }
        }
    }


}

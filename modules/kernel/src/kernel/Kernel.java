package kernel;


import rescuecore2.Constants;
import rescuecore2.Timestep;
import rescuecore2.components.Agent;
import rescuecore2.components.Simulator;
import rescuecore2.components.Viewer;
import rescuecore2.config.Config;
import rescuecore2.log.CommandsRecord;
import rescuecore2.log.ConfigRecord;
import rescuecore2.log.EndLogRecord;
import rescuecore2.log.FileLogWriter;
import rescuecore2.log.InitialConditionsRecord;
import rescuecore2.log.LogException;
import rescuecore2.log.LogWriter;
import rescuecore2.log.Logger;
import rescuecore2.log.PerceptionRecord;
import rescuecore2.log.StartLogRecord;
import rescuecore2.log.UpdatesRecord;
import rescuecore2.messages.Command;
import rescuecore2.messages.control.KASense;
import rescuecore2.messages.control.KSCommands;
import rescuecore2.messages.control.KSUpdate;
import rescuecore2.messages.control.KVTimestep;
import rescuecore2.score.ScoreFunction;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

//import rescuecore2.misc.gui.ChangeSetComponent;

/**
   The Robocup Rescue kernel.
 */
public class Kernel {
    /** The log context for kernel log messages. */
    public static final String KERNEL_LOG_CONTEXT = "kernel";

    private Config config;
    private Perception perception;
    private CommunicationModel communicationModel;
    private WorldModel<? extends Entity> worldModel;
    private LogWriter log;

    private Set<KernelListener> listeners;

    private Collection<AgentProxy> agentProxies;
    private Collection<SimulatorProxy> simProxies;
    private Collection<ViewerProxy> viewerProxies;
    private int time;
    private Timestep previousTimestep;

    private EntityIDGenerator idGenerator;
    private CommandFilter commandFilter;

    private TerminationCondition termination;
    private ScoreFunction score;
    private CommandCollector commandCollector;

    private boolean isShutdown;

    private Collection<Viewer> viewers;
    public List<Simulator> sims;
    private List<Agent> agents;
    private Collection<Command> agentCommands;

    //    private ChangeSetComponent simulatorChanges;

    /**
       Construct a kernel.
       @param config The configuration to use.
       @param perception A perception calculator.
       @param communicationModel A communication model.
       @param worldModel The world model.
       @param idGenerator An EntityIDGenerator.
       @param commandFilter An optional command filter. This may be null.
       @param termination The termination condition.
       @param score The score function.
       @param collector The CommandCollector to use.
       @throws KernelException If there is a problem constructing the kernel.
    */
    public Kernel(Config config,
                  Perception perception,
                  CommunicationModel communicationModel,
                  WorldModel<? extends Entity> worldModel,
                  EntityIDGenerator idGenerator,
                  CommandFilter commandFilter,
                  TerminationCondition termination,
                  ScoreFunction score,
                  CommandCollector collector) throws KernelException {
        try {
            Logger.pushLogContext(KERNEL_LOG_CONTEXT);
            this.config = config;
            this.perception = perception;
            this.communicationModel = communicationModel;
            this.worldModel = worldModel;
            this.commandFilter = commandFilter;
            this.score = score;
            this.termination = termination;
            this.commandCollector = collector;
            this.idGenerator = idGenerator;
            listeners = new HashSet<KernelListener>();
            agentProxies = new TreeSet<AgentProxy>(new Comparator<AgentProxy>() {
                @Override
                public int compare(AgentProxy o1, AgentProxy o2) {
                    return Integer.compare(o1.hashCode(), o2.hashCode());
                }
            });
            simProxies = new TreeSet<SimulatorProxy>(new Comparator<SimulatorProxy>() {
                @Override
                public int compare(SimulatorProxy sp1, SimulatorProxy sp2) {
                    return sp1.getName().compareTo(sp2.getName());
                }
            });
            viewerProxies = new HashSet<ViewerProxy>();
            time = 0;
            try {
                if (!config.getBooleanValue("nolog")) {
                    String logName = config.getValue("kernel.logname");
                    Logger.info("Logging to " + logName);
                    File logFile = new File(logName);
                    if (logFile.getParentFile().mkdirs()) {
                        Logger.info("Created log directory: " + logFile.getParentFile().getAbsolutePath());
                    }
                    if (logFile.createNewFile()) {
                        Logger.info("Created log file: " + logFile.getAbsolutePath());
                    }
                    log = new FileLogWriter(logFile);
                    log.writeRecord(new StartLogRecord());
                    log.writeRecord(new InitialConditionsRecord(worldModel));
                    log.writeRecord(new ConfigRecord(config));
                }
            }
            catch (IOException e) {
                throw new KernelException("Couldn't open log file for writing", e);
            }
            catch (LogException e) {
                throw new KernelException("Couldn't open log file for writing", e);
            }
            config.setValue(Constants.COMMUNICATION_MODEL_KEY, communicationModel.getClass().getName());
            config.setValue(Constants.PERCEPTION_KEY, perception.getClass().getName());

            //            simulatorChanges = new ChangeSetComponent();

            // Initialise
            perception.initialise(config, worldModel);
            communicationModel.initialise(config, worldModel);
            commandFilter.initialise(config);
            score.initialise(worldModel, config);
            termination.initialise(config);
            commandCollector.initialise(config);

            isShutdown = false;

            viewers = new ArrayList<>();
            sims = new ArrayList<>();
            agents = new ArrayList<>();
            agentCommands = new ArrayList<>();

            Logger.info("Kernel initialised");
            Logger.info("Perception module: " + perception);
            Logger.info("Communication module: " + communicationModel);
            Logger.info("Command filter: " + commandFilter);
            Logger.info("Score function: " + score);
            Logger.info("Termination condition: " + termination);
            Logger.info("Command collector: " + collector);
        }
        finally {
            Logger.popLogContext();
        }
    }

    /**
       Get the kernel's configuration.
       @return The configuration.
    */
    public Config getConfig() {
        return config;
    }

    /**
       Get a snapshot of the kernel's state.
       @return A new KernelState snapshot.
    */
    public KernelState getState() {
        return new KernelState(getTime(), getWorldModel());
    }

    /**
       Add an agent to the system.
       @param agent The agent to add.
    */
    public void addAgentProxy(AgentProxy agent) {
        synchronized (this) {
            agentProxies.add(agent);
        }
        fireAgentAdded(agent);
    }

    public void addAgent(Agent agent) {
        agents.add(agent);
        //todo: check whether we need to call fireAgentAdded()
        Collections.sort(agents, new Comparator<Agent>() {
            @Override
            public int compare(Agent t1, Agent t2) {
                return Integer.compare(t1.getID().hashCode(), t2.getID().hashCode());
            }
        });
    }

    /**
       Remove an agent from the system.
       @param agent The agent to remove.
    */
    public void removeAgentProxy(AgentProxy agent) {
        synchronized (this) {
            agentProxies.remove(agent);
        }
        fireAgentRemoved(agent);
    }

    /**
       Get all agents in the system.
       @return An unmodifiable view of all agents.
    */
    public Collection<AgentProxy> getAllAgentProxies() {
        synchronized (this) {
            return Collections.unmodifiableCollection(agentProxies);
        }
    }

    public void addSimulator(Simulator sim) {
        sim.setEntityIDGenerator(idGenerator);
        sims.add(sim);
        //todo: add fireSimulatorAdded(sim);
    }
    public void removeSimulator(Simulator sim) {
        sims.remove(sim);
        //todo: add fireSimulatorRemoved(sim);
    }
    public Collection<Simulator> getSimulators() {
        return Collections.unmodifiableCollection(sims);
    }

    /**
       Add a simulator to the system.
       @param sim The simulator to add.
    */
    public void addSimulatorProxy(SimulatorProxy sim) {
        synchronized (this) {
            simProxies.add(sim);
            sim.setEntityIDGenerator(idGenerator);
        }
        fireSimulatorAdded(sim);
    }

    /**
       Remove a simulator from the system.
       @param sim The simulator to remove.
    */
    public void removeSimulatorProxy(SimulatorProxy sim) {
        synchronized (this) {
            simProxies.remove(sim);
        }
        fireSimulatorRemoved(sim);
    }

    /**
       Get all simulators in the system.
       @return An unmodifiable view of all simulators.
    */
    public Collection<SimulatorProxy> getAllSimulatorProxies() {
        synchronized (this) {
            return Collections.unmodifiableCollection(simProxies);
        }
    }

    public void addViewer(Viewer viewer) {
        viewers.add(viewer);
        fireViewerAdded(viewer);
    }

    /**
       Add a viewer to the system.
       @param viewer The viewer to add.
    */
    public void addViewerProxy(ViewerProxy viewer) {
        synchronized (this) {
            viewerProxies.add(viewer);
        }
        fireViewerProxyAdded(viewer);
    }

    /**
       Remove a viewer from the system.
       @param viewer The viewer to remove.
    */
    public void removeViewer(ViewerProxy viewer) {
        synchronized (this) {
            viewerProxies.remove(viewer);
        }
        fireViewerRemoved(viewer);
    }

    /**
       Get all viewers in the system.
       @return An unmodifiable view of all viewers.
    */
    public Collection<ViewerProxy> getAllViewers() {
        synchronized (this) {
            return Collections.unmodifiableCollection(viewerProxies);
        }
    }

    /**
       Add a KernelListener.
       @param l The listener to add.
    */
    public void addKernelListener(KernelListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    /**
       Remove a KernelListener.
       @param l The listener to remove.
    */
    public void removeKernelListener(KernelListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    /**
       Get the current time.
       @return The current time.
    */
    public int getTime() {
        synchronized (this) {
            return time;
        }
    }

    /**
       Get the world model.
       @return The world model.
    */
    public WorldModel<? extends Entity> getWorldModel() {
        return worldModel;
    }

    /**
       Find out if the kernel has terminated.
       @return True if the kernel has terminated, false otherwise.
    */
    public boolean hasTerminated() {
        synchronized (this) {
            return isShutdown || termination.shouldStop(getState());
        }
    }

    /**
       Run a single timestep.
       @throws InterruptedException If this thread is interrupted during the timestep.
       @throws KernelException If there is a problem executing the timestep.
       @throws LogException If there is a problem writing the log.
    */
    public double timestep() throws InterruptedException, KernelException, LogException {
        try {
            Logger.pushLogContext(KERNEL_LOG_CONTEXT);
            synchronized (this) {
                if (time == 0) {
                    fireStarted();
                }
                if (isShutdown) {
                    return 0;
                }
                ++time;
                // Work out what the agents can see and hear (using the commands from the previous timestep).
                // Wait for new commands
                // Send commands to simulators and wait for updates
                // Collate updates and broadcast to simulators
                // Send perception, commands and updates to viewers
                Timestep nextTimestep = new Timestep(time);
                Logger.info("Timestep " + time);
                Logger.debug("Sending agent updates");
                long start = System.currentTimeMillis();
                sendAgentUpdates(nextTimestep, previousTimestep == null ? new HashSet<Command>() : previousTimestep.getCommands());
                long perceptionTime = System.currentTimeMillis();
                Logger.debug("Waiting for commands");
                Collection<Command> commands = waitForCommands(time);

                nextTimestep.setCommands(commands);
                if (!config.getBooleanValue("nolog")) {
                    log.writeRecord(new CommandsRecord(time, commands));
                }
                long commandsTime = System.currentTimeMillis();
                Logger.debug("Broadcasting commands");
                ChangeSet changes = sendCommandsToSimulators(time, commands);
                //                simulatorUpdates.show(changes);
                nextTimestep.setChangeSet(changes);
                if (!config.getBooleanValue("nolog")) {
                    log.writeRecord(new UpdatesRecord(time, changes));
                }
                long updatesTime = System.currentTimeMillis();
                // Merge updates into world model
                worldModel.merge(changes);
                long mergeTime = System.currentTimeMillis();
                Logger.debug("Broadcasting updates");
                sendUpdatesToSimulators(time, changes);
                sendToViewers(nextTimestep);
                long broadcastTime = System.currentTimeMillis();
                Logger.debug("Computing score");
                double s = score.score(worldModel, nextTimestep);
                Logger.fatal("time:" + time + " score:" + s);
                long scoreTime = System.currentTimeMillis();
                nextTimestep.setScore(s);
                Logger.info("Timestep " + time + " complete");
                Logger.debug("Score: " + s);
                Logger.debug("Perception took        : " + (perceptionTime - start) + "ms");
                Logger.debug("Agent commands took    : " + (commandsTime - perceptionTime) + "ms");
                Logger.debug("Simulator updates took : " + (updatesTime - commandsTime) + "ms");
                Logger.debug("World model merge took : " + (mergeTime - updatesTime) + "ms");
                Logger.debug("Update broadcast took  : " + (broadcastTime - mergeTime) + "ms");
                Logger.debug("Score calculation took : " + (scoreTime - broadcastTime) + "ms");
                Logger.debug("Total time             : " + (scoreTime - start) + "ms");
                fireTimestepCompleted(nextTimestep);
                previousTimestep = nextTimestep;
                Logger.debug("Commands: " + commands);
                Logger.debug("Timestep commands: " + previousTimestep.getCommands());
                return s;
            }
        }
        finally {
            Logger.popLogContext();
        }
    }

    /**
       Shut down the kernel. This method will notify all agents/simulators/viewers of the shutdown.
    */
    public void shutdown() {
        synchronized (this) {
            if (isShutdown) {
                return;
            }
            Logger.info("Kernel is shutting down");
//            ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//            List<Callable<Object>> callables = new ArrayList<Callable<Object>>();
            for (AgentProxy next : agentProxies) {
                final AgentProxy proxy = next;
//                callables.add(Executors.callable(new Runnable() {
//                        @Override
//                        public void run() {
//                            proxy.shutdown();
//                        }
//                    }));
                proxy.shutdown();
            }
            for (SimulatorProxy next : simProxies) {
                final SimulatorProxy proxy = next;
//                callables.add(Executors.callable(new Runnable() {
//                        @Override
//                        public void run() {
//                            proxy.shutdown();
//                        }
//                    }));
                proxy.shutdown();
            }
            for (ViewerProxy next : viewerProxies) {
                final ViewerProxy proxy = next;
//                callables.add(Executors.callable(new Runnable() {
//                        @Override
//                        public void run() {
//                            proxy.shutdown();
//                        }
//                    }));
                proxy.shutdown();
            }
//            try {
//                service.invokeAll(callables);
//                service.shutdown();
//            }
//            catch (InterruptedException e) {
//                Logger.warn("Interrupted during shutdown");
//            }
            try {
                if (!config.getBooleanValue("nolog")) {
                    log.writeRecord(new EndLogRecord());
                    log.close();
                }
            }
            catch (LogException e) {
                Logger.error("Error closing log", e);
            }
            Logger.info("Kernel has shut down");
            isShutdown = true;
            fireShutdown();
        }
    }

    private void sendAgentUpdates(Timestep timestep, Collection<Command> commandsLastTimestep) throws InterruptedException, KernelException, LogException {
        perception.setTime(time);
        communicationModel.process(time, commandsLastTimestep);
        for (AgentProxy next : agentProxies) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            ChangeSet visible = perception.getVisibleEntities(next);
            Collection<Command> heard = communicationModel.getHearing(next.getControlledEntity());
            EntityID id = next.getControlledEntity().getID();
            timestep.registerPerception(id, visible, heard);
            if (!config.getBooleanValue("nolog")) {
                log.writeRecord(new PerceptionRecord(time, id, visible, heard));
            }
            next.sendPerceptionUpdate(time, visible, heard);
        }

        agentCommands.clear();
        for (Agent agent : agents) {
            ChangeSet visible = perception.getVisibleEntities(agent.getID());
            Collection<Command> heard = communicationModel.getHearing(getWorldModel().getEntity(agent.getID()));
            timestep.registerPerception(agent.getID(), visible, heard);
            if (!config.getBooleanValue("nolog")) {
                log.writeRecord(new PerceptionRecord(time, agent.getID(), visible, heard));
            }
            agent.processSense(new KASense(agent.getID(), time, visible, heard));
            agentCommands.addAll(agent.getLastCommands());
        }
    }

    private Collection<Command> waitForCommands(int timestep) throws InterruptedException {
        Collection<Command> commands = commandCollector.getAgentCommands(agentProxies, timestep);
        // add the commands from agents
        commands.addAll(agentCommands);
        Logger.debug("Raw commands: " + commands);
        commandFilter.filter(commands, getState());
        Logger.debug("Filtered commands: " + commands);

        return commands;
    }

    /**
       Send commands to all simulators and return which entities have been updated by the simulators.
    */
    private ChangeSet sendCommandsToSimulators(int timestep, Collection<Command> commands) throws InterruptedException {
        for (SimulatorProxy next : simProxies) {
            next.sendAgentCommands(timestep, commands);
        }
        // Wait until all simulators have sent updates
        ChangeSet result = new ChangeSet();
        for (SimulatorProxy next : simProxies) {
            Logger.debug("Fetching updates from " + next);
            result.merge(next.getUpdates(timestep));
        }

        for (Simulator sim : sims) {
            ChangeSet simChange = new ChangeSet();
            KSCommands cmds = new KSCommands(sim.getID(), timestep, commands);
            sim.processCommands(cmds, simChange);
            result.merge(simChange);
        }
        return result;
    }

    private void sendUpdatesToSimulators(int timestep, ChangeSet updates) throws InterruptedException {
        for (SimulatorProxy next : simProxies) {
            next.sendUpdate(timestep, updates);
        }
        for (Simulator sim : sims) {
            sim.handleUpdate(new KSUpdate(sim.getID(), timestep, updates));
        }
    }

    private void sendToViewers(Timestep timestep) {
        for (ViewerProxy next : viewerProxies) {
            next.sendTimestep(timestep);
        }
        for (Viewer next : viewers) {
            KVTimestep kvTimestep = new KVTimestep(next.getID(), timestep.getTime(), timestep.getCommands(), timestep.getChangeSet());
            next.handleTimestep(kvTimestep);
        }
    }

    private Set<KernelListener> getListeners() {
        Set<KernelListener> result;
        synchronized (listeners) {
            result = new HashSet<KernelListener>(listeners);
        }
        return result;
    }

    private void fireStarted() {
        for (KernelListener next : getListeners()) {
            next.simulationStarted(this);
        }
    }

    private void fireShutdown() {
        for (KernelListener next : getListeners()) {
            next.simulationEnded(this);
        }
    }

    private void fireTimestepCompleted(Timestep timestep) {
        for (KernelListener next : getListeners()) {
            next.timestepCompleted(this, timestep);
        }
    }

    private void fireAgentAdded(AgentProxy agent) {
        for (KernelListener next : getListeners()) {
            next.agentAdded(this, agent);
        }
    }

    private void fireAgentRemoved(AgentProxy agent) {
        for (KernelListener next : getListeners()) {
            next.agentRemoved(this, agent);
        }
    }

    private void fireSimulatorAdded(SimulatorProxy sim) {
        for (KernelListener next : getListeners()) {
            next.simulatorAdded(this, sim);
        }
    }

    private void fireSimulatorRemoved(SimulatorProxy sim) {
        for (KernelListener next : getListeners()) {
            next.simulatorRemoved(this, sim);
        }
    }
    
    private void fireViewerAdded(Viewer viewer) {
        for (KernelListener next : getListeners()) {
            next.viewerAdded(this, viewer);
        }
    }

    private void fireViewerProxyAdded(ViewerProxy viewer) {
        for (KernelListener next : getListeners()) {
            next.viewerProxyAdded(this, viewer);
        }
    }

    private void fireViewerRemoved(ViewerProxy viewer) {
        for (KernelListener next : getListeners()) {
            next.viewerRemoved(this, viewer);
        }
    }
}

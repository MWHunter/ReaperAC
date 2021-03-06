package ac.grim.grimac.utils.threads;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.data.PredictionData;

import java.util.concurrent.*;

public class CustomThreadPoolExecutor extends ThreadPoolExecutor {

    public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public void runCheck(PredictionData data) {
        CompletableFuture.runAsync(() -> MovementCheckRunner.check(data), this).whenComplete((s, t) -> {
            GrimPlayer player = data.player;

            // Set required variables here just in case of exceptions
            player.movementSpeed = player.tempMovementSpeed;
            player.lastX = player.x;
            player.lastY = player.y;
            player.lastZ = player.z;
            player.lastXRot = player.xRot;
            player.lastYRot = player.yRot;
            player.lastOnGround = player.onGround;
            player.lastClimbing = player.isClimbing;

            player.lastTransactionBeforeLastMovement = player.packetStateData.packetLastTransactionReceived.get();

            player.vehicleForward = player.boatData.nextVehicleForward;
            player.vehicleHorizontal = player.boatData.nextVehicleHorizontal;
            player.boatData.nextVehicleForward = (float) Math.min(0.98, Math.max(-0.98, data.vehicleForward));
            player.boatData.nextVehicleHorizontal = (float) Math.min(0.98, Math.max(-0.98, data.vehicleHorizontal));
            player.horseJump = data.horseJump;

            if (t != null) {
                t.printStackTrace();
            }

            // If the last task was finished and there is another task to run -> run the next task
            // If the last task was finished and there are no more tasks -> let tasksNotFinished signal to immediately add to thread pool on new task
            // If the last task wasn't finished because the server hasn't ticked relevant packets -> add the prediction data back to the queue
            // If there is an exception, just queue the next data
            if (!data.player.isCheckNotReady) {
                if (data.player.tasksNotFinished.getAndDecrement() > 1) {
                    PredictionData nextData;

                    // We KNOW that there is data in the queue
                    // However the other thread increments this value BEFORE adding it to the LinkedQueue
                    // Meaning it could increment the value, we read the queue, and it hasn't been added yet
                    // So we have to loop until it's added
                    try {
                        do {
                            nextData = MovementCheckRunner.queuedPredictions.get(data.player.playerUUID).poll();
                        } while (nextData == null);
                    } catch (NullPointerException exception) {
                        return;
                    }

                    PredictionData finalNextData = nextData;
                    runCheck(finalNextData);
                }
            } else {
                MovementCheckRunner.waitingOnServerQueue.add(data);
            }
        });
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        //predictionTime.put(r, System.nanoTime());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }
    }
}

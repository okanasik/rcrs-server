package rescuecore2.standard.score;

import rescuecore2.Timestep;
import rescuecore2.config.Config;
import rescuecore2.score.AbstractScoreFunction;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.WorldModel;

/**
   Score function that measures the number of living civilians. One point per civilian still alive.
 */
public class CiviliansAliveScoreFunction extends AbstractScoreFunction {
    /**
       Construct a CiviliansAliveScoreFunction.
    */
    public CiviliansAliveScoreFunction() {
        super("Civilians alive");
    }

    @Override
    public void initialise(WorldModel<? extends Entity> world, Config config) {
    }

    @Override
    public double score(WorldModel<? extends Entity> world, Timestep timestep) {
        double sum = 1;
        // TODO: we can improve the performance if we iterate over civilians only
        for (Entity next : world) {
            if (next instanceof Civilian) {
                Civilian c = (Civilian)next;
                if (c.isHPDefined() && c.getHP() > 0) {
                    ++sum;
                }
            }
        }
        return sum;
    }
}

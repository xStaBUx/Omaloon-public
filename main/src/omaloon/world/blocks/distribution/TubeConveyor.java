package omaloon.world.blocks.distribution;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;

import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;

import omaloon.content.blocks.*;
import omaloon.utils.*;

import static arc.Core.*;

//TODO topRegion for planing
public class TubeConveyor extends Conveyor {
    public static final int[][] tiles = new int[][] { new int[] {},
            new int[] {0, 2}, new int[] {1, 3}, new int[] {0, 1},
            new int[] {0, 2}, new int[] {0, 2}, new int[] {1, 2},
            new int[] {0, 1, 2}, new int[] {1, 3}, new int[] {0, 3},
            new int[] {1, 3}, new int[] {0, 1, 3}, new int[] {2, 3},
            new int[] {0, 2, 3}, new int[] {1, 2, 3}, new int[] {0, 1, 2, 3}
    };

    public TextureRegion[][] topRegion;
    public TextureRegion[] capRegion;
    public Block junctionReplacement;

    public TubeConveyor(String name) {
        super(name);
    }

    @Override
    public void init() {
        super.init();

        if(junctionReplacement == null) junctionReplacement = OlDistributionBlocks.tubeJunction;
    }

    @Override
    public Block getReplacement(BuildPlan req, Seq<BuildPlan> plans) {
        if (junctionReplacement == null) return this;

        Boolf<Point2> cont = p -> plans.contains(o -> o.x == req.x + p.x && o.y == req.y + p.y && (req.block instanceof TubeConveyor || req.block instanceof Junction));
        return cont.get(Geometry.d4(req.rotation)) &&
                cont.get(Geometry.d4(req.rotation - 2)) &&
                req.tile() != null &&
                req.tile().block() instanceof TubeConveyor &&
                Mathf.mod(req.tile().build.rotation - req.rotation, 2) == 1 ? junctionReplacement : this;
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        super.drawPlanRegion(plan, list);
    }

    @Override
    public void load() {
        super.load();
        topRegion = OlUtils.splitLayers(name + "-sheet", 32, 2);
        capRegion = new TextureRegion[] {topRegion[1][0], topRegion[1][1]};
        uiIcon = atlas.find(name + "-icon");
    }

    public class TubeConveyorBuild extends ConveyorBuild {
        public int tiling = 0;

        public Building buildAt(int i) {
            return nearby(i);
        }

        public boolean valid(int i) {
            Building b = buildAt(i);
            return b != null && (b instanceof TubeConveyorBuild ? (b.front() != null && b.front() == this ) : b.block.acceptsItems);
        }

        public boolean isEnd(int i) {
            var b = buildAt(i);
            return !valid(i) && (b == null ? null : b.block) != this.block;
        }

        @Override
        public void draw() {
            super.draw();
            Draw.z(Layer.blockAdditive);
            Draw.rect(topRegion[0][tiling], x, y, 0);
            int[] placementID = tiles[tiling];
            for(int i : placementID) {
                if(isEnd(i)) {
                    int id = i == 0 || i == 3 ? 1 : 0;
                    Draw.rect(capRegion[id], x, y, i == 0 || i == 2 ? 0 : -90);
                }
            }
        }

        @Override
        public void unitOn(Unit unit) {
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            noSleep();
            next = front();
            nextc = next instanceof TubeConveyorBuild d ? d : null;

            tiling = 0;
            for(int i = 0; i < 4; i++){
                if(i == rotation || valid(i)) {
                    tiling |= (1 << i);
                }
            }
        }
    }
}
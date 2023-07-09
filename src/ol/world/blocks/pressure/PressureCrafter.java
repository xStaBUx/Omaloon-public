package ol.world.blocks.pressure;

import arc.func.Floatf;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.world.blocks.production.GenericCrafter;
import ol.pressure.ConsumePressure;
import ol.pressure.PUtil;
import ol.pressure.PressureModule;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unchecked")
public class PressureCrafter extends GenericCrafter implements IPressureBuild.Block {
    public float overloadDamageMultiplayer = PUtil.DEFAULT_ODM;
    public float tier = PUtil.ANY_TIER_VALUE;
    public ConsumePressure consPressure;
    public boolean hasPressure = true;
    public float pressureProduce = 0;

    public PressureCrafter(String name) {
        super(name);
    }

    public void consumePressureBuffered(float capacity) {
        consumePressure(0, capacity);
    }

    public void consumePressure(float _usage, float _capacity) {
        consumePressureDynamic((ignored) -> _usage, (ignored) -> _capacity);
    }

    public void consumePressureDynamic(Floatf<Building> _usage, Floatf<Building> _capacity) {
        consume(new ConsumePressure() {{
            this.usage = _usage;
            this.capacity = _capacity;
        }});
    }

    @Override
    public boolean isPressure() {
        return pressureProduce > 0 && consPressure != null;
    }

    public class PressureCrafterBuild extends GenericCrafterBuild implements IPressureBuild {
        private final PressureModule pressure = new PressureModule();
        private float timer = 0;

        public boolean timer(int period) {
            return Mathf.floor(timer) % period == 0;
        }

        @Override
        public void craft() {
            super.craft();
            if(pressureProduce > 0 && pressure() != null) {
                pressure().graph.pushPressure(pressureProduce);
            }
        }

        @Override
        public @NotNull PressureCrafterBuild self() {
            return this;
        }

        @Override
        public float tier() {
            return tier;
        }

        @Override
        public boolean hasPressure() {
            return hasPressure;
        }

        @Override
        public PressureModule pressure() {
            return pressure;
        }

        @Override
        public ConsumePressure consPressure() {
            return consPressure;
        }

        @Override
        public float overloadDamageMultiplayer() {
            return overloadDamageMultiplayer;
        }

        @Override
        public void updateTile() {
            super.updateTile();
            timer += Time.delta;
            if(timer(10)) { //6 times per second handle will be normal
                var p = pressure();
                if(p.graph.isOverload()) {
                    handleOverload();
                }
            }
        }

        @Override
        public void onDestroyed() {
            super.onDestroyed();
            destroyD();
        }

        @Override
        public void afterPickedUp() {
            super.afterPickedUp();
            pickedUpD();
        }

        @Override
        public void placed() {
            super.placed();
            placedD();
        }

        @Override
        public void read(Reads read, byte ignored) {
            var p = pressure();
            if(p != null) {
                p.read(read);
            }
        }

        @Override
        public void write(Writes write) {
            var p = pressure();
            if(p != null) {
                p.write(write);
            }
        }
    }
}

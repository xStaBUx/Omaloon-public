package omaloon.world.blocks.liquid;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.sandbox.*;
import omaloon.content.*;
import omaloon.utils.*;
import omaloon.world.interfaces.*;
import omaloon.world.meta.*;
import omaloon.world.modules.*;

import static mindustry.Vars.*;
import static mindustry.type.Liquid.*;

public class PressureLiquidValve extends LiquidBlock {
	public PressureConfig pressureConfig = new PressureConfig();

	public TextureRegion[] tiles;
	public TextureRegion[][] liquidRegions;
	public TextureRegion valveRegion, topRegion;

	public Effect disperseEffect = OlFx.valveSpray;
	public float disperseEffectInterval = 30;

	public float pressureLoss = 0.05f, liquidLoss = 0.05f;

	public float openMin = -15f, openMax = 15f;

	public float liquidPadding = 3f;

	public PressureLiquidValve(String name) {
		super(name);
		rotate = true;
	}

	@Override
	public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
		var tiling = new Object() {
			int tiling = 0;
		};
		Point2
			front = new Point2(1, 0).rotate(plan.rotation).add(plan.x, plan.y),
			back = new Point2(-1, 0).rotate(plan.rotation).add(plan.x, plan.y);

		boolean inverted = plan.rotation == 1 || plan.rotation == 2;
		list.each(next -> {
			if (new Point2(next.x, next.y).equals(front) && next.block.outputsLiquid) tiling.tiling |= inverted ? 2 : 1;
			if (new Point2(next.x, next.y).equals(back) && next.block.outputsLiquid) tiling.tiling |= inverted ? 1 : 2;
		});

		Draw.rect(bottomRegion, plan.drawx(), plan.drawy(), 0);
		Draw.rect(tiles[tiling.tiling], plan.drawx(), plan.drawy(), (plan.rotation + 1) * 90f % 180 - 90);
		Draw.rect(valveRegion, plan.drawx(), plan.drawy(), (plan.rotation + 1) * 90f % 180 - 90);
		Draw.rect(topRegion, plan.drawx(), plan.drawy());
	}

	@Override
	public TextureRegion[] icons() {
		return new TextureRegion[]{bottomRegion, region};
	}

	@Override
	public void load() {
		super.load();
		tiles = OlUtils.split(name + "-tiles", 32, 0);
		valveRegion = Core.atlas.find(name + "-valve");
		topRegion = Core.atlas.find(name + "-top");
		if (!bottomRegion.found()) bottomRegion = Core.atlas.find("omaloon-liquid-bottom");

		liquidRegions = new TextureRegion[2][animationFrames];
		if(renderer != null){
			var frames = renderer.getFluidFrames();

			for (int fluid = 0; fluid < 2; fluid++) {
				for (int frame = 0; frame < animationFrames; frame++) {
					TextureRegion base = frames[fluid][frame];
					TextureRegion result = new TextureRegion();
					result.set(base);

					result.setHeight(result.height - liquidPadding);
					result.setWidth(result.width - liquidPadding);
					result.setX(result.getX() + liquidPadding);
					result.setY(result.getY() + liquidPadding);

					liquidRegions[fluid][frame] = result;
				}
			}
		}
	}

	@Override
	public void setBars() {
		super.setBars();
		pressureConfig.addBars(this);
	}

	@Override
	public void setStats() {
		super.setStats();
		pressureConfig.addStats(stats);
	}

	public class PressureLiquidValveBuild extends LiquidBuild implements HasPressure {
		PressureModule pressure = new PressureModule();

		public float draining;
		public float effectInterval;
		public int tiling;

		@Override
		public boolean acceptLiquid(Building source, Liquid liquid) {
			return hasLiquids;
		}

		@Override
		public boolean canDumpLiquid(Building to, Liquid liquid) {
			return super.canDumpLiquid(to, liquid) || to instanceof LiquidVoid.LiquidVoidBuild;
		}

		@Override
		public boolean connects(HasPressure to) {
			return to instanceof PressureLiquidValveBuild ?
				       (front() == to || back() == to) && (to.front() == this || to.back() == this) :
				       (front() == to || back() == to);
		}

		@Override
		public void draw() {
			float rot = rotate ? (90 + rotdeg()) % 180 - 90 : 0;
			Draw.rect(bottomRegion, x, y, rotation);
			if (liquids().currentAmount() > 0.01f) {
				int frame = liquids.current().getAnimationFrame();
				int gas = liquids.current().gas ? 1 : 0;

				float xscl = Draw.xscl, yscl = Draw.yscl;
				Draw.scl(1f, 1f);
				Drawf.liquid(liquidRegions[gas][frame], x, y, liquids.currentAmount()/liquidCapacity, liquids.current().color.write(Tmp.c1).a(1f));
				Draw.scl(xscl, yscl);
			}
			Draw.rect(tiles[tiling], x, y, rot);
			Draw.rect(topRegion, x, y);
			Draw.rect(valveRegion, x, y, draining * (rotation%2 == 0 ? -90 : 90) + rot);
		}

		@Override
		public void onProximityAdded() {
			super.onProximityAdded();
			pressureGraph().addBuild(this);
		}

		@Override
		public void onProximityRemoved() {
			super.onProximityRemoved();
			pressureGraph().removeBuild(this, true);
		}

		@Override
		public void onProximityUpdate() {
			super.onProximityUpdate();
			tiling = 0;
			boolean inverted = rotation == 1 || rotation == 2;
			if (front() instanceof HasPressure front && connects(front)) tiling |= inverted ? 2 : 1;
			if (back() instanceof HasPressure back && connects(back)) tiling |= inverted ? 1 : 2;
			pressureGraph().removeBuild(this, false);
		}

		@Override public PressureModule pressure() {
			return pressure;
		}
		@Override public PressureConfig pressureConfig() {
			return pressureConfig;
		}

		@Override
		public void updateDeath() {
			HasPressure.super.updateDeath();
			if (getPressure() >= openMax) {
				effectInterval += delta();
				removePressure(pressureLoss * Time.delta);
				if (liquids.current() != null) liquids.remove(liquids.current(), liquidLoss * delta());
				draining = Mathf.approachDelta(draining, 1, 0.014f);
			}
			if (getPressure() <= openMin) {
				handlePressure(pressureLoss * Time.delta);
				draining = Mathf.approachDelta(draining, 1, 0.014f);
			}
			draining = Mathf.approachDelta(draining, 0, 0.014f);
			if (effectInterval > disperseEffectInterval && liquids.currentAmount() > 0.1f) {
				effectInterval = 0;
				disperseEffect.at(x, y, draining * (rotation%2 == 0 ? -90 : 90) + (rotate ? (90 + rotdeg()) % 180 - 90 : 0), liquids.current());
			}
		}

		@Override
		public void updateTile() {
			updateDeath();
			nextBuilds(true).each(b -> moveLiquidPressure(b, liquids.current()));
			dumpPressure();
		}

		@Override
		public void read(Reads read, byte revision) {
			super.read(read, revision);
			pressure.read(read);
		}
		@Override
		public void write(Writes write) {
			super.write(write);
			pressure.write(write);
		}
	}
}

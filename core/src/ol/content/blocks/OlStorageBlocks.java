package ol.content.blocks;

import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import ol.content.*;

import static mindustry.type.ItemStack.*;

public class OlStorageBlocks {
    public static Block
            //cores
            landingCapsule;

    public static void load(){
        //cores
        landingCapsule = new CoreBlock("landing-capsule"){{
            requirements(Category.effect, BuildVisibility.sandboxOnly, empty);

            isFirstTier = true;
            unitType = OlUnitTypes.discoverer;
            health = 650;
            itemCapacity = 450;
            size = 2;

            unitCapModifier = 3;
        }};
    }
}
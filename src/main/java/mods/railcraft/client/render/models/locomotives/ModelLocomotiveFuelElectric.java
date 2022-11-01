
package mods.railcraft.client.render.models.locomotives;

import mods.railcraft.client.render.models.ModelSimple;
import net.minecraft.client.model.ModelRenderer;

public class ModelLocomotiveFuelElectric extends ModelSimple {

    public ModelLocomotiveFuelElectric() {
        super("loco");

        renderer.setTextureSize(96, 96);

       setTextureOffset("loco.wheels", 1, 77);
       setTextureOffset("loco.frame", 1, 1);
       setTextureOffset("loco.front", 67, 76);
       setTextureOffset("loco.engine", 1, 22);
       setTextureOffset("loco.lightA", 1, 6);
       setTextureOffset("loco.lightB", 1, 1);
       setTextureOffset("loco.top", 1, 52);
       setTextureOffset("loco.ventA", 68, 26);
       setTextureOffset("loco.ventB", 1, 70);
       
       renderer.rotationPointX = 8F;
       renderer.rotationPointY = 8F;
       renderer.rotationPointZ = 8F;
       
       ModelRenderer loco = renderer;
       loco.addBox("wheels", -20F, -5F, -16F, 23, 2, 16);
       loco.addBox("frame", -21F, -7F, -17F, 25, 2, 18);
       loco.addBox("front", -22F, -8F, -14F, 2, 4, 12);
       loco.addBox("engine", -20F, -19F, -16F, 23, 12, 16);
       loco.addBox("lightA", -21F, -11F, -4F, 1, 2, 2);
       loco.addBox("lightB", -21F, -11F, -14F, 1, 2, 2);
       loco.addBox("top", -18F, -21F, -14F, 22, 3, 12);
       loco.addBox("ventA", -16F, -22F, -11F, 6, 1, 6);
       loco.addBox("ventB", -7F, -22F, -10F, 9, 1, 4);
    }	
}

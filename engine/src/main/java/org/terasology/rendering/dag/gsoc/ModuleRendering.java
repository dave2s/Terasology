/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.dag.gsoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.context.Context;
import org.terasology.engine.SimpleUri;
import org.terasology.engine.module.ModuleManager;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.naming.Name;
import org.terasology.registry.In;
import org.terasology.rendering.dag.RenderGraph;
import org.terasology.rendering.opengl.FBO;
import org.terasology.rendering.opengl.FboConfig;
import org.terasology.rendering.opengl.ScalingFactors;
import org.terasology.rendering.world.WorldRenderer;

@RegisterSystem
public abstract class ModuleRendering extends BaseComponentSystem {
    protected static final Logger logger = LoggerFactory.getLogger(ModuleRendering.class);

    @In
    protected Context context;
    protected Name providingModule;
    protected RenderGraph renderGraph;
    protected WorldRenderer worldRenderer;

    // @Override
    public void initialise(Class clazz) {
        super.initialise();
        setProvidingModule(clazz);
        renderGraph = context.get(RenderGraph.class);
        worldRenderer = context.get(WorldRenderer.class);
    }

    protected void setProvidingModule(Class implementingClass) {
        ModuleManager moduleManager = context.get(ModuleManager.class);
        this.providingModule = moduleManager.getEnvironment().getModuleProviding(implementingClass);
    }

    protected BufferPair createBufferPair(String primaryBufferName, String secondaryBufferName,
                                          ScalingFactors primaryBufferScale, ScalingFactors secondaryBufferScale,
                                          FBO.Type primaryBufferType, FBO.Type secondaryBufferType, FBO.Dimensions scale) {

        FBO buffer1 = generateWithDimensions(new FboConfig(new SimpleUri(providingModule + ":fbo." + primaryBufferName), primaryBufferScale, primaryBufferType)
                .useDepthBuffer().useNormalBuffer().useLightBuffer().useStencilBuffer(), scale);
        FBO buffer2 = generateWithDimensions(new FboConfig(new SimpleUri(providingModule + ":fbo." + secondaryBufferName), secondaryBufferScale, secondaryBufferType)
                .useDepthBuffer().useNormalBuffer().useLightBuffer().useStencilBuffer(), scale);
        return new BufferPair(buffer1, buffer2);
    }

    /**
     * Generates and returns an FBO as characterized by the FboConfig and the dimensions arguments.
     *
     * Notice that if the name of the FBO being generated matches the name of an FBO already stored
     * by the manager, the latter will be overwritten. However, the GPU-side Frame Buffer associated
     * with the overwritten FBO is not disposed by this method.
     *
     * As such, this method should be used only after the relevant checks are made and any
     * pre-existing FBO with the same name as the new one is appropriately disposed.
     *
     * This method produces errors in the log in case the FBO generation process results in
     * FBO.Status.INCOMPLETE or FBO.Status.UNEXPECTED.
     *
     * @param fboConfig an FboConfig object providing FBO configuration details.
     * @param dimensions an FBO.Dimensions instance providing the dimensions of the FBO.
     * @return an FBO instance
     */
    protected FBO generateWithDimensions(FboConfig fboConfig, FBO.Dimensions dimensions) {
        fboConfig.setDimensions(dimensions);
        FBO fbo = FBO.create(fboConfig);

        // At this stage it's unclear what should be done in this circumstances as I (manu3d) do not know what
        // the effects of using an incomplete FrameBuffer are. Throw an exception? Live with visual artifacts?
        if (fbo.getStatus() == FBO.Status.INCOMPLETE) {
            logger.error("FBO " + fboConfig.getName() + " is incomplete. Look earlier in the log for details.");
        } else if (fbo.getStatus() == FBO.Status.UNEXPECTED) {
            logger.error("FBO " + fboConfig.getName() + " has generated an unexpected status code. Look earlier in the log for details.");
        }
        return fbo;
    }

}

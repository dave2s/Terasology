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
package org.terasology.logic.players;

import org.terasology.engine.Time;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityBuilder;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.actions.PlaySoundActionComponent;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.characters.CharacterHeldItemComponent;
import org.terasology.logic.console.commandSystem.annotations.Command;
import org.terasology.logic.console.commandSystem.annotations.CommandParam;
import org.terasology.logic.location.Location;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.network.ClientComponent;
import org.terasology.registry.In;
import org.terasology.rendering.logic.VisualComponent;
import org.terasology.rendering.world.WorldRenderer;

import java.util.HashMap;
import java.util.Map;

@RegisterSystem(RegisterMode.CLIENT)
public class ThirdPersonRemoteClientSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final int USEANIMATIONLENGTH = 200;

    @In
    private LocalPlayer localPlayer;
    @In
    private WorldRenderer worldRenderer;
    @In
    private EntityManager entityManager;
    @In
    private Time time;

    private Map<EntityRef,EntityRef> charactersHandEntities = new HashMap<>();

    private Map<EntityRef,EntityRef> charactersHeldItems = new HashMap<>();

    // the item from the inventory synchronized with the server
    //private EntityRef currentHeldItem = EntityRef.NULL;

    private EntityRef getHandEntity(EntityRef character) {
        EntityRef handEntity = charactersHandEntities.get(character);
        if ( handEntity == null) {
            // create the hand entity
            EntityBuilder entityBuilder = entityManager.newBuilder("engine:hand");
            entityBuilder.setPersistent(false);
            handEntity = entityBuilder.build();
            charactersHandEntities.put(character,handEntity);
        }
        return handEntity;
    }

    // ensures held item mount point entity exists, attaches it to the camera and sets its transform
    @ReceiveEvent
    public void ensureClientSideEntityOnHeldItemMountPoint(OnActivatedComponent event, EntityRef character,
                                                           RemotePersonHeldItemMountPointComponent remotePersonHeldItemMountPointComponent) {
      if (!remotePersonHeldItemMountPointComponent.mountPointEntity.exists()) {
            EntityBuilder builder = entityManager.newBuilder("engine:RemotePersonHeldItemMountPoint");
            builder.setPersistent(false);
            remotePersonHeldItemMountPointComponent.mountPointEntity = builder.build();
            character.saveComponent(remotePersonHeldItemMountPointComponent);
        }

        // link the mount point entity to the camera
        Location.removeChild(character, remotePersonHeldItemMountPointComponent.mountPointEntity);
        Location.attachChild(character, remotePersonHeldItemMountPointComponent.mountPointEntity,
                remotePersonHeldItemMountPointComponent.translate,
                new Quat4f(
                        TeraMath.DEG_TO_RAD * remotePersonHeldItemMountPointComponent.rotateDegrees.y,
                        TeraMath.DEG_TO_RAD * remotePersonHeldItemMountPointComponent.rotateDegrees.x,
                        TeraMath.DEG_TO_RAD * remotePersonHeldItemMountPointComponent.rotateDegrees.z),
                remotePersonHeldItemMountPointComponent.scale);
    }

    @ReceiveEvent
    public void ensureHeldItemIsMountedOnLoad(OnChangedComponent event, EntityRef clientEntity, ClientComponent clientComponent) {
        //if(entityRef instanceof )
        if ((!(localPlayer.getClientEntity().equals(clientEntity))) &&(clientEntity.exists()) && clientComponent.character!= EntityRef.NULL) {
            CharacterHeldItemComponent characterHeldItemComponent = clientComponent.character.getComponent(CharacterHeldItemComponent.class);
            if (characterHeldItemComponent != null && !(clientComponent.character.equals(localPlayer.getCharacterEntity()))) {
                linkHeldItemLocationForRemotePlayer(characterHeldItemComponent.selectedItem, clientComponent.character);
            }
        }
    }

    @Command(shortDescription = "Sets the held item mount point translation for the first person view")
    public void setRemotePlayersHeldItemMountPointTranslations(@CommandParam("x") float x, @CommandParam("y") float y, @CommandParam("z") float z) {
        for (EntityRef remotePlayer: charactersHeldItems.keySet()) {
            RemotePersonHeldItemMountPointComponent newComponent = remotePlayer.getComponent(RemotePersonHeldItemMountPointComponent.class);
            if (newComponent != null) {
                newComponent.translate = new Vector3f(x, y, z);
                ensureClientSideEntityOnHeldItemMountPoint(OnActivatedComponent.newInstance(), remotePlayer, newComponent);
            }
        }
    }

    @Command(shortDescription = "Sets the held item mount point rotation for the first person view")
    public void setRemotePlayersHeldItemMountPointRotations(@CommandParam("x") float x, @CommandParam("y") float y, @CommandParam("z") float z) {
        for (EntityRef remotePlayer: charactersHeldItems.keySet()) {
            RemotePersonHeldItemMountPointComponent newComponent = remotePlayer.getComponent(RemotePersonHeldItemMountPointComponent.class);
            if (newComponent != null) {
                newComponent.rotateDegrees = new Vector3f(x, y, z);
                ensureClientSideEntityOnHeldItemMountPoint(OnActivatedComponent.newInstance(), remotePlayer, newComponent);
            }
        }
    }

    @ReceiveEvent
    public void onHeldItemActivated(OnActivatedComponent event, EntityRef character, CharacterHeldItemComponent heldItemComponent, CharacterComponent characterComponents) {
        if (!localPlayer.getCharacterEntity().equals(character)) {
            EntityRef newItem = heldItemComponent.selectedItem;
            linkHeldItemLocationForRemotePlayer(newItem,character);
        }
    }

    @ReceiveEvent
    public void onHeldItemChanged(OnChangedComponent event, EntityRef character, CharacterHeldItemComponent heldItemComponent, CharacterComponent characterComponents) {
        if (!localPlayer.getCharacterEntity().equals(character)) {
            EntityRef newItem = heldItemComponent.selectedItem;
            linkHeldItemLocationForRemotePlayer(newItem,character);
        }
    }

    /**
     * Changes held item entity.
     *
     * <p>Detaches old held item and removes it's components. Adds components to new held item and
     * attaches it to the mount point entity.</p>
     */
    private void linkHeldItemLocationForRemotePlayer(EntityRef newItem,EntityRef character) {
        //If this character is yet unknown to us, add it to the map
        if(!charactersHeldItems.containsKey(character)){
            charactersHeldItems.put(character,EntityRef.NULL);
        }
        EntityRef currentHeldItem = charactersHeldItems.get(character);
        if (!newItem.equals(currentHeldItem/*!newItem.equals(item)*/)) {
            //EntityRef camera = localPlayer.getCameraEntity();
            RemotePersonHeldItemMountPointComponent mountPointComponent = character.getComponent(RemotePersonHeldItemMountPointComponent.class);
            if (mountPointComponent != null) {

                //currentHeldItem is at this point the old item
                if (currentHeldItem != EntityRef.NULL) {
                    currentHeldItem.destroy();
                }

                // use the hand if there is no new item
                EntityRef newHeldItem;
                if (newItem == EntityRef.NULL) {
                    newHeldItem = getHandEntity(character);
                } else {
                    newHeldItem = newItem;
                }

                // create client side held item entity for remote player and store it in local remote players map
                currentHeldItem = entityManager.create();
                charactersHeldItems.putIfAbsent(character,currentHeldItem);

                // add the visually relevant components
                for (Component component : newHeldItem.iterateComponents()) {
                    if (component instanceof VisualComponent && !(component instanceof FirstPersonHeldItemTransformComponent)) {
                        currentHeldItem.addComponent(component);
                        //character.addComponent(component);
                    }
                }

                // ensure world location is set
                currentHeldItem.addComponent(new LocationComponent());
                currentHeldItem.addComponent(new ItemIsHeldComponent());

                RemotePersonHeldItemTransformComponent heldItemTransformComponent = currentHeldItem.getComponent(RemotePersonHeldItemTransformComponent.class);
                if (heldItemTransformComponent == null) {
                    heldItemTransformComponent = new RemotePersonHeldItemTransformComponent();
                    currentHeldItem.addComponent(heldItemTransformComponent);
                }

                Location.attachChild(mountPointComponent.mountPointEntity, currentHeldItem,
                        heldItemTransformComponent.translate,
                        new Quat4f(
                                TeraMath.DEG_TO_RAD * heldItemTransformComponent.rotateDegrees.y,
                                TeraMath.DEG_TO_RAD * heldItemTransformComponent.rotateDegrees.x,
                                TeraMath.DEG_TO_RAD * heldItemTransformComponent.rotateDegrees.z),
                        heldItemTransformComponent.scale);
            }
        }
    }

    private void updateRemoteCharacters(){
        for (EntityRef entityRef : entityManager.getEntitiesWith(CharacterComponent.class)) {
            if (/*(!entityRef.equals(localPlayer)) &&*/ (!charactersHeldItems.containsKey(entityRef))) {
                charactersHeldItems.put(entityRef,EntityRef.NULL);
            }
        }

        for (EntityRef entityRef : entityManager.getEntitiesWith(CharacterComponent.class)) {
            if (/*(!entityRef.equals(localPlayer)) &&*/ (!charactersHandEntities.containsKey(entityRef))) {
                charactersHandEntities.put(entityRef,EntityRef.NULL);
            }
        }

        /*for (EntityRef entityRef : charactersHeldItems.keySet()){
            if(entityManager.getEntitiesWith(CharacterComponent.class)  <- if not contains, remove here
        }*/
    }

    /**
     * modifies the held item mount point to move the held item in first person view
     */
    @Override
    public void update(float delta) {
        //Ensure remote clients are stored in the map
        //TODO probably not good to call here but make sure it's called upon held item activation? or client disconnect and add removing entries
        updateRemoteCharacters();

        for (EntityRef remotePlayer: charactersHeldItems.keySet()) {
            if(remotePlayer.equals(localPlayer.getCharacterEntity())){
                continue;
            }
            EntityRef currentHeldItem = charactersHeldItems.get(remotePlayer);
            // ensure empty hand is shown if no item is held at the moment
            if (!currentHeldItem.exists() && currentHeldItem != getHandEntity(remotePlayer)) {
                linkHeldItemLocationForRemotePlayer(getHandEntity(remotePlayer), remotePlayer);
            }
        }
            //TODO this seems rather redundant in this cycle, extract
            // ensure that there are no lingering items that are marked as still held. This situation happens with client side predicted items
          /*  for (EntityRef entityRef : entityManager.getEntitiesWith(ItemIsHeldComponent.class)) {
                if (!charactersHeldItems.containsValue(entityRef)) {
                    entityRef.destroy();
                }
            }*/
        for (EntityRef remotePlayer: charactersHeldItems.keySet()) {
            if(remotePlayer.equals(localPlayer.getCharacterEntity())){
                continue;
            }
            //EntityRef currentHeldItem = charactersHeldItems.get(remotePlayer);

            // get the first person mount point and rotate it away from the camera
            CharacterHeldItemComponent characterHeldItemComponent = remotePlayer.getComponent(CharacterHeldItemComponent.class);
            RemotePersonHeldItemMountPointComponent mountPointComponent = remotePlayer.getComponent(RemotePersonHeldItemMountPointComponent.class);
            if (characterHeldItemComponent == null
                    || mountPointComponent == null) {
                return;
            }

            LocationComponent locationComponent = mountPointComponent.mountPointEntity.getComponent(LocationComponent.class);
            if (locationComponent == null) {
                return;
            }

            long timeElapsedSinceLastUsed = time.getGameTimeInMs() - characterHeldItemComponent.lastItemUsedTime;
            float animateAmount = 0f;
            if (timeElapsedSinceLastUsed < USEANIMATIONLENGTH) {
                // half way through the animation will be the maximum extent of rotation and translation
                animateAmount = 1f - Math.abs(((float) timeElapsedSinceLastUsed / (float) USEANIMATIONLENGTH) - 0.5f);
            }
            float addPitch = 15f * animateAmount;
            float addYaw = 10f * animateAmount;
            locationComponent.setLocalRotation(new Quat4f(
                    TeraMath.DEG_TO_RAD * (mountPointComponent.rotateDegrees.y + addYaw),
                    TeraMath.DEG_TO_RAD * (mountPointComponent.rotateDegrees.x + addPitch),
                    TeraMath.DEG_TO_RAD * mountPointComponent.rotateDegrees.z));
            Vector3f offset = new Vector3f(0.25f * animateAmount, -0.12f * animateAmount, 0f);
            offset.add(mountPointComponent.translate);
            locationComponent.setLocalPosition(offset);

            mountPointComponent.mountPointEntity.saveComponent(locationComponent);
        }
    }

    @Override
    public void preSave() {
       /* if (currentHeldItem != EntityRef.NULL) {
            currentHeldItem.destroy();
        }*/

    }
}

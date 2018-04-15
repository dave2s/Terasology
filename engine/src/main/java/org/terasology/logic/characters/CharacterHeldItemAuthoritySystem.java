/*
 * Copyright 2015 MovingBlocks
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
package org.terasology.logic.characters;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.events.ChangeHeldItemRequest;
/*import org.terasology.network.FieldReplicateType;
import org.terasology.network.Replicate;

import java.util.HashMap;
import java.util.Map;*/

@RegisterSystem(RegisterMode.AUTHORITY)
public class CharacterHeldItemAuthoritySystem extends BaseComponentSystem {

    /*@Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public Map<EntityRef,CharacterHeldItemComponent> charactersHeldItems = new HashMap<>();
*/
    @ReceiveEvent
    public void onChangeHeldItemRequest(ChangeHeldItemRequest event, EntityRef character,
                                        CharacterHeldItemComponent characterHeldItemComponent) {
        characterHeldItemComponent.selectedItem = event.getItem();
        //updateCharactersHeldItems(character, characterHeldItemComponent);
        character.saveComponent(characterHeldItemComponent);
    }

   /* private void updateCharactersHeldItems(EntityRef character, CharacterHeldItemComponent characterHeldItemComponent){
        if (!charactersHeldItems.containsKey(character)){
            charactersHeldItems.put(character,characterHeldItemComponent);
        }
        else{
            charactersHeldItems.putIfAbsent(character,characterHeldItemComponent);
        }
    }*/

   /* @Override
    public void preSave() {
        charactersHeldItems.clear();
    }*/
}

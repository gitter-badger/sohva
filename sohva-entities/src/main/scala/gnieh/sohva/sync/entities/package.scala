/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gnieh.sohva
package sync

import gnieh.sohva.entities.Entity

import spray.json._

/** This package exposes classes that allows user to manage entities and their
 *  components within a CouchDB database.
 *
 *  Entities are conceptually a simple identifier. In database they are stored as
 *  a simple document that has a single optional `tag` field.
 *  The components are stored in their own document as well.
 *  The [[gnieh.sohva.sync.entities.EntityManager]] also manages views that allow for
 *  retrieving entities and their components.
 */
package object entities {

  implicit class RichEntity(val entity: Entity) extends AnyVal {

    /** Adds the given component to the entity */
    def save[T <: IdRev: Manifest: JsonFormat](component: T)(implicit manager: EntityManager): T =
      manager.saveComponent(entity, component)

    /** Removes the given component to the entity */
    def remove[T <: IdRev: Manifest: JsonFormat](component: T)(implicit manager: EntityManager): Boolean =
      manager.removeComponent(entity, component)

    /** Removes all components of a given type attached to the entity */
    def remove[T: Manifest](implicit manager: EntityManager): Boolean =
      manager.removeComponentType[T](entity)

    /** Indicates whether the entity has at least one component of the given name */
    def has[T: Manifest](implicit manager: EntityManager): Boolean =
      manager.hasComponentType[T](entity)

    /** Indicates whether the entity has the given component */
    def has[T: Manifest: JsonFormat](component: T)(implicit manager: EntityManager): Boolean =
      manager.hasComponent(entity, component)

    /** Gets the component of the given type attached to the entity if any */
    def get[T: Manifest: JsonFormat](implicit manager: EntityManager): Option[T] =
      manager.getComponent[T](entity)

    /** Removes the entity from the system */
    def delete(implicit manager: EntityManager): Boolean =
      manager.deleteEntity(entity)

  }

}

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

import com.google.common.collect.Maps;
import org.terasology.engine.SimpleUri;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class DependencyConnection<T> {

    private String connectionName;
    private Type connectionType;
    private SimpleUri parentNode;
    private HashMap<String, DependencyConnection> connectedConnections;
    private T data;

    DependencyConnection(String name, Type type, SimpleUri parentNode) {
        this.connectionName = name;
        this.connectionType = type;
        this.parentNode = parentNode;
        // connectedNode = null;
        connectedConnections = Maps.newHashMap();
    }

    public T getData() {
        return this.data;
    }

    protected void setData(T data) {
        this.data = data;
    }

    public enum Type { // Might differentiate by name only
        INPUT,
        OUTPUT
    }

    /**
     * Getter for a SimpleUri name of DependencyConnection.parentNode. This attribute should always contain a name
     * of a node object this node has been attributed to.
     * @return A name of the node this connection has been attributed to. This must not be null.
     */
    @Nonnull
    public SimpleUri getParentNode() {
        return this.parentNode;
    }

    /**
     * Getter for a DependencyConnection connection of connected node. The idea is that when connecting a toNode's input to another
     * fromNode's output, you query the OUTPUT connection for its connectedNode. And vice-versa when you want to e.g. remove a node and you need
     * to know whether it's connected to some dependencies.
     * @return A node name if this Connection has been connected to another node's connection. Null if the output
     * has not yet been connected.
     */
    @Nullable
    public HashMap<String, DependencyConnection> getConnectedConnections() {
        return connectedConnections;
    }

    /**
     * Setter for a DependencyConnection connection of DependencyConnection.connectedConnection. The idea is that when connecting a toNode's input to another
     * fromNode's output, you set the OUTPUT connection's connectedConnection to the toNode's DependencyConnection which you connect.
     * @param connectedConnection A DependenyConnection connection of connected node.
     */
    public void setConnectedConnection(DependencyConnection connectedConnection) {
        connectedConnections.putIfAbsent(connectedConnection.getName(), connectedConnection);
    }

    protected void setParentNode(SimpleUri parentNode) {
        this.parentNode = parentNode;
    }

    public String getName() {
        return this.connectionName;
    }

    public Type getType() {
        return this.connectionType;
    }

    /**
     * Removes current link for both ends of the connection, sets connected node to connectToConnection and gets its data
     * @param fromConnection
     */
    public void connectInputToOutput(DependencyConnection<T> fromConnection) {
        /*if (this.connectedConnection != null) {
            this.connectedConnection.connectedConnection = null;
        }*/
        this.connectedConnections.putIfAbsent(fromConnection.getName(), fromConnection);
        this.connectedConnections.get(fromConnection.getName()).getConnectedConnections().putIfAbsent(this.getName(), this);
        this.data = fromConnection.getData();
    }

    /**
     * Remove connections
     */
    public void disconnect() {
        this.connectedConnections.forEach((k,v)->v.connectedConnections.remove(this));
        this.connectedConnections = null;
        if (this.connectionType == Type.INPUT) {
            this.data = null;
        }
    }

    // String getConnectionName(int number);

    @Override
    public String toString() {
        StringBuilder connectedConnectionString = new StringBuilder("");
        connectedConnections.forEach((k,v)->connectedConnectionString.append(k).append(", "));
        connectedConnectionString.append(";");

        return (connectionType == Type.OUTPUT)
                ? String.format("Output:%s(connected to %s)", connectionName, connectedConnectionString.toString())
                : String.format("Input:%s(connected to %s)", connectionName, connectedConnectionString.toString());
    }

    /*
     * Constructs DependencyConnection name based on ID and Dep.connection type.
     * @param id
     * @return
     */
    // public abstract String getConnectionName(int id);

}

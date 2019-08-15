package com.infy.estquido;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WayPoint {
    private String id;
    private Node node;
    private Vector3 position;
    private Set<WayPoint> connections;
    private boolean isSelected;

    public WayPoint(String id, Vector3 position) {
        this.id = id;
        this.position = position;
        this.node = new Node();
        this.connections = new HashSet<>();
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public Set<WayPoint> getConnections() {
        return connections;
    }

    public Vector3 getPosition() {
        return position;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("x", position.x);
        map.put("y", position.y);
        map.put("z", position.z);
        List<String> list = new ArrayList<>();
        connections.forEach(l -> list.add(l.id));
        map.put("connections", list);

        return map;
    }

    @NonNull
    @Override
    public String toString() {
        return toMap().toString();
    }


}
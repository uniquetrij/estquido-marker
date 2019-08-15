package com.infy.estquido;

import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableArray;
import com.couchbase.lite.MutableDocument;
import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class CheckpointsActivity extends AppCompatActivity {


    private static final String TAG = CheckpointsActivity.class.getName();
    private static final String DB_NAME = "Estquido";

    private ArFragment mArFragment;
    private Vector3 mCamPosition;


    private Set<WayPoint> mWayPoints = Collections.synchronizedSet(new LinkedHashSet<>());
    private WayPoint selectedWayPoint;

    private int wayPointCounter = 0;
    private Database database;
    private MutableDocument document;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkpoints);
        initialiseDB();

        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.checkpoints_fragment);

        mArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Camera mARCamera = mArFragment.getArSceneView().getScene().getCamera();
            mCamPosition = mARCamera.getWorldPosition();
        });
    }

    private void initialiseDB() {
        DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());
        try {
            database = new Database(DB_NAME, config);
            Document doc = database.getDocument("B32F0");

            if (doc == null) {
                document = new MutableDocument("B32F0");
                document.setValue("WayPoints", new ArrayList<Map<String, Object>>());
                document.setValue("WayPointIDs", new ArrayList<Integer>());
                database.save(document);
            } else {
                document = doc.toMutable();
                ((MutableArray) document.getValue("WayPoints")).toList().stream().forEachOrdered(m -> {
                    Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>) m;
                    Map<String, Object> wpMap = map.values().stream().findFirst().get();
                    WayPoint wayPoint = new WayPoint(((Long) wpMap.get("id")).intValue(), new Vector3((float) wpMap.get("x"), (float) wpMap.get("y"), (float) wpMap.get("z")));
                    mWayPoints.add(wayPoint);
                });
                wayPointCounter = ((MutableArray) document.getValue("WayPointIDs")).toList().stream().mapToInt(value -> ((Long) value).intValue()).max().getAsInt();


            }
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        Log.d("DATABASE", wayPointCounter + "");
        Log.d("DATABASE", ((MutableArray) document.getValue("WayPoints")).toList().toString());
        Log.d("DATABASE", mWayPoints.toString());
    }



    public void placeWayPoint(View view) {
        mWayPoints.add(addWayPoint(++wayPointCounter, mCamPosition));
    }

    private WayPoint addWayPoint(Integer id, Vector3 position) {
        WayPoint wayPoint = new WayPoint(id, position);
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")))
                .thenAccept(material -> {
                    ModelRenderable modelRenderable = ShapeFactory.makeSphere(0.1f, new Vector3(0.0f, 0.0f, 0.0f), material);
                    Anchor anchor = mArFragment.getArSceneView().getSession().createAnchor(new Pose(new float[]{position.x, position.y, position.z}, new float[]{0, 0, 0, 0}));
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(mArFragment.getArSceneView().getScene());

                    wayPoint.getNode().setParent(anchorNode);
                    wayPoint.getNode().setRenderable(modelRenderable);
                    wayPoint.getNode().setOnTapListener(new Node.OnTapListener() {
                        @Override
                        public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                            if (!wayPoint.isSelected()) {
                                if (selectedWayPoint == null) {
                                    wayPoint.setSelected(true);
                                    wayPoint.getNode().getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.BLUE));
                                    selectedWayPoint = wayPoint;
                                } else {
                                    selectedWayPoint.setSelected(false);
                                    connectWayPoints(selectedWayPoint, wayPoint);
                                    selectedWayPoint.getNode().getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")));
                                    selectedWayPoint = null;

                                }
                            } else {
                                wayPoint.setSelected(false);
                                wayPoint.getNode().getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")));
                                selectedWayPoint = null;
                            }
                        }
                    });
                });
        return wayPoint;
    }

    private void connectWayPoints(WayPoint from, WayPoint to) {
        if (from.getConnections().contains(to) && to.getConnections().contains(from))
            return;

        from.getConnections().add(to);
        to.getConnections().add(from);

        AnchorNode node1 = (AnchorNode) from.getNode().getParent();
        AnchorNode node2 = (AnchorNode) to.getNode().getParent();
        Vector3 point1, point2;
        point1 = node1.getWorldPosition();
        point2 = node2.getWorldPosition();

        final Vector3 difference = Vector3.subtract(point1, point2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new com.google.ar.sceneform.rendering.Color(Color.RED))
                .thenAccept(material -> {
                            ModelRenderable model = ShapeFactory.makeCube(new Vector3(0.025f, 0.025f, difference.length()), Vector3.zero(), material);
                            Node node = new Node();
                            node.setParent(node1);
                            node.setRenderable(model);
                            node.setWorldPosition(Vector3.add(point1, point2).scaled(.5f));
                            node.setWorldRotation(rotationFromAToB);

                            node.setOnTapListener(new Node.OnTapListener() {
                                @Override
                                public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                                    node.setParent(null);
                                    from.getConnections().remove(to);
                                    to.getConnections().remove(from);
                                }
                            });
                        }
                );
    }

    public void syncPositions(View view) {
        view.setEnabled(false);
        syncPositions();
        view.setEnabled(true);
    }

    public void syncPositions() {
        mArFragment.getArSceneView().getSession().getAllAnchors().forEach(anchor -> anchor.detach());
        Set<WayPoint> oldWP = Collections.synchronizedSet(new LinkedHashSet<>(mWayPoints));
        Map<Integer, WayPoint> newWayPoints = Collections.synchronizedMap(new LinkedHashMap<>());
        mWayPoints.clear();

        oldWP.stream().forEachOrdered(wayPoint -> {
            newWayPoints.put(wayPoint.getId(), addWayPoint(wayPoint.getId(), wayPoint.getPosition()));
        });

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Set<WayPoint> visited = new HashSet<>();
                        oldWP.stream().forEachOrdered(wayPoint -> {
                            visited.add(wayPoint);
                            wayPoint.getConnections().stream().forEachOrdered(wayPoint1 -> {
                                if (!visited.contains(wayPoint1)) {
                                    connectWayPoints(newWayPoints.get(wayPoint.getId()), newWayPoints.get(wayPoint1.getId()));
                                }
                            });
                        });
                    }
                });
            }
        }, 10);

        mWayPoints.addAll(newWayPoints.values());
    }

    public void persistWayPoints(View view) {
        List<Map<String, Object>> wpArray = new ArrayList<>();
        List<Integer> idArray = new ArrayList<>();
        mWayPoints.stream().forEachOrdered(wayPoint -> {
            Map<String, Object> node = new LinkedHashMap<>();
            Map<String, Object> map = wayPoint.toMap();
            node.put(wayPoint.getId() + "", map);
            wpArray.add(node);
            idArray.add(wayPoint.getId());
        });
        document.setValue("WayPoints", wpArray);
        document.setValue("WayPointIDs", idArray);
        try {
            database.save(document);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }
}

package com.mygame;

import com.jme3.anim.AnimComposer;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.UniformBinding;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowRenderer;
import com.jme3.system.AppSettings;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */

public class Main extends SimpleApplication implements ActionListener {
    
    private Joystick viewedJoystick;

    private Node joystickInfo;
    private float yInfo = 0;
    private JoystickButton lastButton;
    
    private BulletAppState bulletAppState;
    private Spatial autoModel;
    //private Node autoModel;
    private VehicleControl vehicle;
    private final float accelerationForce = 1500.0f;
    private final float brakeForce = 100.0f;
    private float steeringValue = 0;
    private float accelerationValue = 0;
    final private Vector3f jumpForce = new Vector3f(0, 3000, 0);
    private Node scene;
    private PointLight red, blue;
    private final Vector3f redPos = new Vector3f();
    private final Vector3f bluePos = new Vector3f();
    private Vector3f redDir = new Vector3f();
    private  Vector3f blueDir = new Vector3f();
    private final Vector3f LIGHT_OFFSET_TO_BLUE = new Vector3f(0.5f,-2,0f);
    
    private final float lightsAlpha = 0.75f;
    private Material matTerrain;
    
    private boolean throttle;
    
    public static void main(String[] args) {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setUseJoysticks(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {      
        //scene = (Node)assetManager.loadModel("Scenes/Scene_1.j3o");   
        scene = (Node)assetManager.loadModel("Scenes/Scene_3.j3o");        
        rootNode.attachChild(scene);
        System.out.println("Game loaded");
        flyCam.setMoveSpeed(flyCam.getMoveSpeed()*30f);
        loadPhysic();
        Node terrain = (Node)scene.getChild("Terrain");        
        RigidBodyControl rbc1 = new RigidBodyControl(0f);                  
        terrain.addControl(rbc1);
        rbc1.getCollisionShape().setContactFilterEnabled(false); 
        bulletAppState.getPhysicsSpace().addAll(terrain);
        setupKeys();
        buildPlayer();
        loadLights();       
        terrain.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        createShadowsForSecondary();   
        loadGamepad();
        rootNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    }
    
    private void setupKeys() {
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "Lefts");
        inputManager.addListener(this, "Rights");
        inputManager.addListener(this, "Ups");
        inputManager.addListener(this, "Downs");
        inputManager.addListener(this, "Space");
        inputManager.addListener(this, "Reset");
    }

    @Override
    public void simpleUpdate(float tpf) {
        
        Spatial child = autoModel;
        child.setLocalTranslation(vehicle.getPhysicsLocation());
        child.setLocalRotation(vehicle.getPhysicsRotation());  
        
  
        flyCam.setEnabled(false);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    private void loadPhysic() {
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setAccuracy(1f/60f);
        //PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
        
        bulletAppState.getPhysicsSpace().setGravity(bulletAppState.getPhysicsSpace().getGravity(redPos).mult(3f));
    }
    
    private void buildPlayer() {
        Material mat = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");      
        mat.getAdditionalRenderState().setWireframe(true);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.setTransparent(true);
        mat.setColor("Color", ColorRGBA.Red);
        CompoundCollisionShape compoundShape = new CompoundCollisionShape();
        BoxCollisionShape box = new BoxCollisionShape(new Vector3f(1.0f, 0.5f, 2.4f));
        compoundShape.addChildShape(box, new Vector3f(0, 1, 0));
        //create vehicle node
        Node vehicleNode =new Node("vehicleNode");
        vehicle = new VehicleControl(compoundShape, 150);
        float maxRadius = compoundShape.maxRadius();
        vehicle.setCcdMotionThreshold(maxRadius);
        vehicle.setCcdSweptSphereRadius(maxRadius);
        
        
        vehicleNode.addControl(vehicle);

        float stiffness = 60.0f;//200=f1 car
        float compValue = .3f; //(should be lower than damp)
        float dampValue = .4f;
        vehicle.setSuspensionCompression(compValue * 2.0f * FastMath.sqrt(stiffness));
        vehicle.setSuspensionDamping(dampValue * 2.0f * FastMath.sqrt(stiffness));
        vehicle.setSuspensionStiffness(stiffness);
        vehicle.setMaxSuspensionForce(10000.0f);

        //Create four wheels and add them at their locations
        Vector3f wheelDirection = new Vector3f(0, -1, 0); // was 0, -1, 0
        Vector3f wheelAxle = new Vector3f(-1, 0, 0); // was -1, 0, 0
        float radius = 0.55f;
        float restLength = 0.40f;   //dist to the wheel from the body
        float yOff = 0.65f;
        float xOff = 2.095f;
        float zOff = 2f;

        Cylinder wheelMesh = new Cylinder(16, 16, radius, radius * 2.6f, true);
        wheelMesh.setMode(Mesh.Mode.Points);

        Node node1 = new Node("wheel 1 node");
        Geometry wheels1 = new Geometry("wheel 1", wheelMesh);
        node1.attachChild(wheels1);
        wheels1.rotate(0, FastMath.HALF_PI, 0);
        wheels1.setMaterial(mat);
        //wheels1.
        vehicle.addWheel(node1, new Vector3f(-xOff, yOff, zOff),
                wheelDirection, wheelAxle, restLength, radius, true);

        Node node2 = new Node("wheel 2 node");
        Geometry wheels2 = new Geometry("wheel 2", wheelMesh);
        node2.attachChild(wheels2);
        wheels2.rotate(0, FastMath.HALF_PI, 0);
        wheels2.setMaterial(mat);
        vehicle.addWheel(node2, new Vector3f(xOff, yOff, zOff),
                wheelDirection, wheelAxle, restLength, radius, true);

        Node node3 = new Node("wheel 3 node");
        Geometry wheels3 = new Geometry("wheel 3", wheelMesh);
        node3.attachChild(wheels3);
        wheels3.rotate(0, FastMath.HALF_PI, 0);
        wheels3.setMaterial(mat);
        vehicle.addWheel(node3, new Vector3f(-xOff, yOff, -zOff),
                wheelDirection, wheelAxle, restLength, radius, false);

        Node node4 = new Node("wheel 4 node");
        Geometry wheels4 = new Geometry("wheel 4", wheelMesh);
        node4.attachChild(wheels4);
        wheels4.rotate(0, FastMath.HALF_PI, 0);
        wheels4.setMaterial(mat);
        vehicle.addWheel(node4, new Vector3f(xOff, yOff, -zOff),
                wheelDirection, wheelAxle, restLength, radius, false);

        vehicleNode.attachChild(node1);
        vehicleNode.attachChild(node2);
        vehicleNode.attachChild(node3);
        vehicleNode.attachChild(node4);
        rootNode.attachChild(vehicleNode);

        getPhysicsSpace().add(vehicle);     
        
        autoModel = (Spatial)scene.getChild("Auto_2");   
        autoModel.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //Mesh mesh = (Node)
        vehicleNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //autoModel.setCullHint(Spatial.CullHint.Always);
        //autoModel = assetManager.loadModel("Models/Auto/Auto.j3o");
        //getRootNode().attachChild(autoModel);  
        
        vehicle.setPhysicsLocation(autoModel.getLocalTranslation());  
        
        ChaseCamera chaseCam = new ChaseCamera(cam, autoModel, inputManager);
        //ChaseCamera chaseCam = new ChaseCamera(cam, (Spatial)autoModel.getChild("Auto_1"), inputManager);
        chaseCam.setSmoothMotion(true);
        chaseCam.setDefaultHorizontalRotation(0.5f);
        chaseCam.setRotationSpeed(chaseCam.getRotationSpeed()*5f);
        chaseCam.setRotationSensitivity(chaseCam.getRotationSensitivity()*5f);
        //chaseCam.set
        chaseCam.setDefaultVerticalRotation(3.14f/12f);
           chaseCam.setRotationSensitivity(chaseCam.getRotationSensitivity()*105f);
           //chaseCam.setDefaultHorizontalRotation(12f);
        chaseCam.setRotationSpeed(chaseCam.getRotationSpeed()*10f);
    }
    
    private PhysicsSpace getPhysicsSpace(){
        return bulletAppState.getPhysicsSpace();
    }
    
    @Override
    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Lefts")) {
            if (value) {
                steeringValue += .25f;
            } else {
                steeringValue += -.25f;
            }
            vehicle.steer(steeringValue);
        } else if (binding.equals("Rights")) {
            if (value) {
                steeringValue += -.25f;
            } else {
                steeringValue += .25f;
            }
            vehicle.steer(steeringValue);
        } else if (binding.equals("Ups")) {
            if (value) {
                accelerationValue += accelerationForce;
            } else {
                accelerationValue -= accelerationForce;
            }
            vehicle.accelerate(accelerationValue);
        } else if (binding.equals("Downs")) {
            if (value) {
                vehicle.brake(brakeForce);
            } else {
                vehicle.brake(0f);
            }
        } else if (binding.equals("Space")) {
            if (value) {
                vehicle.applyImpulse(jumpForce, Vector3f.ZERO);
            }
        } else if (binding.equals("Reset")) {
            if (value) {
                System.out.println("Reset");
                vehicle.setPhysicsLocation(Vector3f.ZERO);
                vehicle.setPhysicsRotation(new Matrix3f());
                vehicle.setLinearVelocity(Vector3f.ZERO);
                vehicle.setAngularVelocity(Vector3f.ZERO);
                vehicle.resetSuspension();
            }
            else {


            }
        }
    }

    private void loadLights() {
        
        
            try{
        System.out.println(" Start to load lights ");
        var lights = scene.getLocalLightList();
        for (int i = 0; i < lights.size(); i++){
             if (lights.get(i) instanceof PointLight point){
                PointLightShadowRenderer sunShadowRenderer = new PointLightShadowRenderer(getAssetManager(), 1024*4);
                sunShadowRenderer.setLight(point);
                viewPort.addProcessor(sunShadowRenderer);
                System.out.println("Point light added");
            }
             else if (lights.get(i) instanceof DirectionalLight directional){
                DirectionalLightShadowRenderer sunShadowRenderer = new DirectionalLightShadowRenderer(getAssetManager(), 1024*2,3);
                sunShadowRenderer.setLight(directional);
                viewPort.addProcessor(sunShadowRenderer);
                System.out.println("Directional Light added");
            }
        }
                 
            }
            catch (Exception e){
                System.out.println("Can not load shadows. " + e.getLocalizedMessage());
            }
    }

    private void createShadowsForSecondary() {
        try{
            
            for (int i = 0; i < scene.getChildren().size(); i++){
                System.out.println("Obj: " + i + " has name: " + scene.getChild(i).getName());
            }
            
            scene.getChild("Shield").setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            
            Node fenceParent = (Node)scene.getChild("Fence");             
            fenceParent.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);  
            Geometry fenceGeometry = (Geometry)fenceParent.getChild(0);
            fenceGeometry.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);  
            System.out.println("Fence makes shadows");
            
            Geometry shieldGeometry = (Geometry)scene.getChild("Shield");
            shieldGeometry.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);  
            System.out.println("Shield makes shadows");
            
            
           
            
            
            Node scientistParent = (Node)scene.getChild("Scientist");
            Node armature = (Node)scientistParent.getChild("Armature");
            Node scientistChild = (Node)armature.getChild("scientist");
            
             AnimComposer composer = armature.getControl(AnimComposer.class);
             composer.setCurrentAction("idle_4");
             composer.setGlobalSpeed(composer.getGlobalSpeed()*3f);
             
            Geometry scientist_0 = (Geometry)scientistChild.getChild("scientist_0");            
            scientist_0.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            
            Geometry scientist_1 = (Geometry)scientistChild.getChild("scientist_1");            
            scientist_1.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            
            
            Geometry autoGeometry = (Geometry)scene.getChild("Auto_2");
            autoGeometry.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            System.out.println("Car makes shadows");
        }
        catch(Exception e){
            e.printStackTrace();
        }
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void loadGamepad() {
        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks == null)
            throw new IllegalStateException("Cannot find any joysticks!");

        try {
            PrintWriter out = new PrintWriter( new FileWriter( "joysticks-" + System.currentTimeMillis() + ".txt" ) );
            dumpJoysticks( joysticks, out );
            out.close();
        } catch( IOException e ) {
            throw new RuntimeException( "Error writing joystick dump", e );
        }   


        int gamepadSize = cam.getHeight() / 2;
        float scale = gamepadSize / 512.0f;        


        joystickInfo = new Node( "joystickInfo" );
        joystickInfo.setLocalTranslation( 0, cam.getHeight(), 0 );

        inputManager.addRawInputListener( new JoystickEventListener() );
        
        // add action listener for mouse click 
        // to all easier custom mapping
        inputManager.addMapping("mouseClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if(isPressed){

                    log("PRessed vent");
                }
            }
        }, "mouseClick");
    }
    
    
    protected void dumpJoysticks( Joystick[] joysticks, PrintWriter out ) {
        
    }
       
    
    
    protected void addInfo( String info, int column ) {
    
        BitmapText t = new BitmapText(guiFont);
        t.setText( info );
        t.setLocalTranslation( column * 200, yInfo, 0 );
        //joystickInfo.attachChild(t);
        yInfo -= t.getHeight();
    }
    
    protected void setViewedJoystick( Joystick stick ) {
        if( this.viewedJoystick == stick )
            return;
 
        if( this.viewedJoystick != null ) {
            joystickInfo.detachAllChildren();
        }
                   
        this.viewedJoystick = stick;
 
        if( this.viewedJoystick != null ) {       
            // Draw the hud
            yInfo = 0;
 
            addInfo(  "Joystick:\"" + stick.getName() + "\"  id:" + stick.getJoyId(), 0 );
 
            yInfo -= 5;
                       
            float ySave = yInfo;
            
            // Column one for the buttons
            addInfo( "Buttons:", 0 );
            for( JoystickButton b : stick.getButtons() ) {
                addInfo( " '" + b.getName() + "' id:'" + b.getLogicalId() + "'", 0 );
            }
            yInfo = ySave;
            
            // Column two for the axes
            addInfo( "Axes:", 1 );
            for( JoystickAxis a : stick.getAxes() ) {
                addInfo( " '" + a.getName() + "' id:'" + a.getLogicalId() + "' analog:" + a.isAnalog(), 1 );
            }
            
        } 
    }
    
    private void log(String log){
        System.out.println("DATA: " + log);
    }
    
    protected class JoystickEventListener implements RawInputListener {

        final private Map<JoystickAxis, Float> lastValues = new HashMap<>();

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
            Float last = lastValues.remove(evt.getAxis());
            float value = evt.getValue();
                    
            // Check the axis dead zone.  InputManager normally does this
            // by default but not for raw events like we get here.
            float effectiveDeadZone = Math.max(inputManager.getAxisDeadZone(), evt.getAxis().getDeadZone());
            if( Math.abs(value) < effectiveDeadZone ) {
                if( last == null ) {
                    // Just skip the event
                    return;
                }
                // Else set the value to 0
                lastValues.remove(evt.getAxis());
                value = 0;
            }         
            setViewedJoystick( evt.getAxis().getJoystick() );            
            //gamepad.setAxisValue( evt.getAxis(), value );
            log("Axis event: " + value + "; Axis: " + evt.getAxis().getName());
            if (evt.getAxis().getName().equals("pov_x")){
               log("Axis!");
                 if (value>(-0.03f) && value < 0.03f) {
                    steeringValue= 0f;
                } else {
                    steeringValue= -(float)(value)/6f;
                }
            vehicle.steer(steeringValue);
            }
            if( value != 0 ) {
                lastValues.put(evt.getAxis(), value);
            } 
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
            setViewedJoystick( evt.getButton().getJoystick() );
            //gamepad.setButtonValue( evt.getButton(), evt.isPressed() );
        }

        @Override
        public void beginInput() {}
        @Override
        public void endInput() {}
        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {}
        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {}
        @Override
        public void onKeyEvent(KeyInputEvent evt) {}
        @Override
        public void onTouchEvent(TouchEvent evt) {}        
    }

}





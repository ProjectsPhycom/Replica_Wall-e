package com.example.controlbluetooh;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final static String UP = "w";
    final static String DOWN = "s";
    final static String RIGHT = "d";
    final static String LEFT = "a";

    //Bluetooth
    private BluetoothAdapter btAdapter = null;// Adapter del bluetooth del celular
    private BluetoothSocket btSocket = null;
    public static String address = null;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public boolean activar;
    Handler bluetoothIn;
    final int handlerState = 0;
    private ConnectedThread MyConexionBT;
    Button btnConnect, btnDisconnect;

    //Animacion
    static Animation scaleDown_up, scaleUp_up, scaleDown_down, scaleUp_down, scaleDown_left, scaleUp_left, scaleDown_right, scaleUp_right;
    //Controles
    static ImageButton btnUp, btnDown, btnLeft, btnRight;
    SeekBar leftArm, rightArm;
    //Posicion de progreso de los SeekBar
    int rightPosition, leftPosition;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Se define solo vista horizontal
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // Barras para control de los brazos
        leftArm = findViewById(R.id.leftArm);
        rightArm = findViewById(R.id.rightArm);
        leftPosition = leftArm.getProgress();
        rightPosition = rightArm.getProgress();

        /*bluetoothIn = new Handler(){
          public void handleMenssage(android.os.Menssage msg){
              if(msg.what == handlerState){
                  //Interaccion con datos de ingreso

              }
          }
        };*/
        bluetoothIn = new Handler();
        //************************ Animaci??n *************************
        // Botones direccionales
        btnUp = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        setEnableControls(false);

        // Se hace una animaci??n para cada boton porque de lo contrario se pueden cruzar las animaciones...
        // Ej: Se suelta right y la animacion la puede ejecutar ese boton y alg??n otro como up
        scaleDown_up = AnimationUtils.loadAnimation(this, R.anim.scale_down); scaleDown_down = AnimationUtils.loadAnimation(this, R.anim.scale_down);
        scaleDown_right = AnimationUtils.loadAnimation(this, R.anim.scale_down); scaleDown_left = AnimationUtils.loadAnimation(this, R.anim.scale_down);
        scaleUp_up = AnimationUtils.loadAnimation(this, R.anim.scale_up); scaleUp_down = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        scaleUp_right = AnimationUtils.loadAnimation(this, R.anim.scale_up); scaleUp_left = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        setConfiguration_to_buttons();

        //******************* Conexi??n bluetooth *******************
        System.out.println("configurando bluetooth");
        //Botones de conecci??n
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        switchConectionButtonsEnable(ConnectedBT.DISCONNECTED);

        // Set<BluetoothDevice> pairedDeveicesList = Collections.emptySet();
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btAdapter = BluetoothAdapter.getDefaultAdapter();// Obtiene el adaptador bluetooth del celular
                System.out.println("\n\tAdaptador");
                System.out.println(btAdapter);
                verificarBluetooth();// verifica que el celular tenga activado el bluetooth
                // Tambi??n se puede hacer una activaci??n del bluetooth forzosa, pero no queremos eso
                if(btAdapter.isEnabled()) {
                    // Conjunto de BluetoothDeviceobjetos que est??n vinculados (emparejados) al adaptador local
                    Set<BluetoothDevice> pairedDeveicesList = btAdapter.getBondedDevices();// devuelve null en caso de que el disposiivo no de soporte a coneccion bluetooth
                    System.out.println("\n\tSet de disposotivos emparejados");
                    System.out.println(btAdapter.getState());
                    System.out.println(pairedDeveicesList);
                    if (pairedDeveicesList.size()>0) {
                        // Obtener el address del modulo, va verificando cada uno de las se??ales bluetooth presentes ya empaejadas
                        for (BluetoothDevice pairedDevice : pairedDeveicesList) {
                            System.out.println("\n\taddress...");
                            System.out.println(pairedDevice.getName());

                            if (pairedDevice.getName().equals("HC-05")) {// Encuentra al modulo
                                // El address es la direcci??n de hardware (direcci??n Mac) del adaptador Bluetooth del celular
                                address = pairedDevice.getAddress();
                                System.out.println("\n\taddress encontrado");
                            }
                        }
                        activar = address != null;
                        if(activar) {
                            System.out.print("\n\tIniciando coneccion... activar??->");
                            System.out.println(activar);
                            onResume();
                        }else{//Tengo dispositivos emperejados, pero no al m??dulo bluetooth
                            Toast.makeText(getBaseContext(), "No se ha emparejado con RobotEco (HC-05)", Toast.LENGTH_LONG).show();
                        }
                    }else{//no tengo dispositivos emparejados
                        // Lanzar toast
                        Toast.makeText(getBaseContext(), "No tiene dispositivos emparejados", Toast.LENGTH_LONG).show();
                    }

                }
            }
        });
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btSocket!=null) {
                    try {
                        btSocket.close();
                        setEnableControls(false);
                        switchConectionButtonsEnable(ConnectedBT.DISCONNECTED);
                        // Lanzar toast
                        Toast.makeText(getBaseContext(), "Se desconect?? exitosamente", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //******************* SeekBars - Brazos *******************
        leftArm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(leftPosition!=i) {
                    System.out.println(seekBar.getProgress());
                    System.out.println(i);
                    System.out.println(b);
                    int diference = Math.abs(leftPosition - i);
                    System.out.println("StartingTrack... " + Integer.toString(leftPosition) + " " + Integer.toString(i) + " " + Integer.toString(diference));
                    if (diference > 10) {
                        seekBar.setProgress(leftPosition);// se mantiene en la posicion
                    } else {
                        leftPosition = i;
                        //aqu?? se hace la comunicaci??n serial
                        String mensaje = Integer.toString(i);
                        MyConexionBT.write((i-45)+10);
                        System.out.println("env??a mensaje bi");
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        rightArm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(rightPosition!=i) {
                    System.out.println(seekBar.getProgress());
                    System.out.println(i);
                    System.out.println(b);
                    int diference = Math.abs(rightPosition - i);
                    System.out.println("StartingTrack... " + Integer.toString(rightPosition) + " " + Integer.toString(i) + " " + Integer.toString(diference));
                    if (diference > 10) {
                        seekBar.setProgress(rightPosition);// se mantiene en la posicion
                    } else {
                        rightPosition = i;
                        //aqu?? se hace la comunicaci??n serial
                        String mensaje = Integer.toString(i+100);
                        if((i-45)+150<255){ MyConexionBT.write((i-45)+150);}
                        System.out.println("env??a mensaje bd");
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }


    @SuppressLint("StaticFieldLeak")
    public enum Controls{
        UP(1, btnUp, scaleDown_up, scaleUp_up), DOWN(3, btnDown, scaleDown_down, scaleUp_down),
        LEFT(2, btnLeft, scaleDown_left, scaleUp_left),RIGHT(4, btnRight, scaleDown_right, scaleUp_right);
        //LEFT_ARM("bi"), RIGHT_ARM("bd");

        private int name;
        private Animation scaleUp;
        private Animation scaleDown;
        private ImageButton button;
        private Controls(int name){
            this.name=name;
            scaleUp=null;
            scaleDown=null;
            button=null;
        }
        private Controls(int name, ImageButton button, Animation scaleDown, Animation scaleUp){
            this.name=name;
            this.button=button;
            this.scaleDown=scaleDown;
            this.scaleUp=scaleUp;
        }

        public int getName() {return name;}
        public Animation getScaleUp() {return scaleUp;}
        public Animation getScaleDown() {return scaleDown;}
        public ImageButton getButton() {return button;}
    }



    /**
     * Funcion para habilitar o deshabilitar todos los controles en la app para el robot
     * @param enable
     */
    void setEnableControls(boolean enable){
        float opacity_level = 1;
        if(!enable){ opacity_level = (float) 0.5;}//opacidad en 50% para cuando se deshabiliten los controles
        //Direcciones
        btnUp.setEnabled(enable);
        btnUp.setAlpha(opacity_level);
        btnDown.setEnabled(enable);
        btnDown.setAlpha(opacity_level);
        btnRight.setEnabled(enable);
        btnRight.setAlpha(opacity_level);
        btnLeft.setEnabled(enable);
        btnLeft.setAlpha(opacity_level);
        //SeekBars de brazos
        leftArm.setEnabled(enable);
        leftArm.setAlpha(opacity_level);
        rightArm.setEnabled(enable);
        rightArm.setAlpha(opacity_level);
    }

    void setEnable_forEspecificControls(boolean enableArmL, boolean enableArmR,
                                        boolean enableUp, boolean enableDown, boolean enableLeft, boolean enableRight){
        //SeekBars de brazos
        leftArm.setEnabled(enableArmL);
        rightArm.setEnabled(enableArmR);
        //Direcciones
        btnUp.setEnabled(enableUp);
        btnDown.setEnabled(enableDown);
        btnRight.setEnabled(enableRight);
        btnLeft.setEnabled(enableLeft);
    }

    public enum ConnectedBT{CONNECTED, DISCONNECTED;}

    /**
     * Hace que solo uno de los botones respecto a la conecci??n y desconecci??n del bluetooth est?? habilitado para evitar errores
     * @param state
     */
    void switchConectionButtonsEnable(ConnectedBT state){
        if(state==ConnectedBT.CONNECTED){
            btnConnect.setEnabled(false);
            btnDisconnect.setEnabled(true);
        }else if(state==ConnectedBT.DISCONNECTED){
            btnConnect.setEnabled(true);
            btnDisconnect.setEnabled(false);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setConfiguration_to_buttons(){
        btnUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                excecuteControlActions(Controls.UP, motionEvent);
                System.out.println("Up");
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){//cuando se lo presiona
                    setEnable_forEspecificControls(false, false, true, false, false, false);
                }else if(motionEvent.getAction()==MotionEvent.ACTION_UP){//cuando se lo suelta
                    setEnable_forEspecificControls(true, true, true, true, true, true);
                }
                return true;
            }
        });
        btnDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                excecuteControlActions(Controls.DOWN, motionEvent);
                System.out.println("Down");
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){//cuando se lo presiona
                    setEnable_forEspecificControls(false, false, false, true, false, false);
                }else if(motionEvent.getAction()==MotionEvent.ACTION_UP){//cuando se lo suelta
                    setEnable_forEspecificControls(true, true, true, true, true, true);
                }
                return true;
            }
        });
        btnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                excecuteControlActions(Controls.LEFT, motionEvent);
                System.out.println("Left");
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){//cuando se lo presiona
                    setEnable_forEspecificControls(false, false, false, false, true, false);
                }else if(motionEvent.getAction()==MotionEvent.ACTION_UP){//cuando se lo suelta
                    setEnable_forEspecificControls(true, true, true, true, true, true);
                }
                return true;
            }
        });
        btnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                excecuteControlActions(Controls.RIGHT, motionEvent);
                System.out.println("Right");
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){//cuando se lo presiona
                    setEnable_forEspecificControls(false, false, false, false, false, true);
                }else if(motionEvent.getAction()==MotionEvent.ACTION_UP){//cuando se lo suelta
                    setEnable_forEspecificControls(true, true, true, true, true, true);
                }
                return true;
            }
        });
    }

    private void excecuteControlActions(Controls direction, MotionEvent motionEvent){
        if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){//cuando se lo presiona
            direction.getButton().startAnimation(direction.getScaleDown());// ANIMACION
            MyConexionBT.write(direction.getName());//                  Comunicacion con arduino
        }else if(motionEvent.getAction()==MotionEvent.ACTION_UP){//cuando se lo suelta
            direction.getButton().startAnimation(direction.getScaleUp());// ANIMACION
            MyConexionBT.write(0);//                  Comunicacion con arduino
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////// BLUETOOTH /////////////////////////////////////////////
    private BluetoothSocket createBluetoothSocket (BluetoothDevice device) throws IOException {
        // Crea una conexion de salida segura para el dispositivo usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }


    /**
     * Lanza un Toast en caso de que el bluetooth no est?? activo en el celular
     */
    private void verificarBluetooth(){
        if(btAdapter.isEnabled()){
            //No se hace nada, contin??a con normalidad
            System.out.println("\n\tBluetooth del celular activado");
        }else{
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,1);
        }
    }

    public void onResume() {
        super.onResume();
        System.out.println("\n\tDentro de onResume");
        if (activar) {
            System.out.println("\n\tAdaptador onResume");
            System.out.println(btAdapter);
            if(btAdapter == null){
                btAdapter = BluetoothAdapter.getDefaultAdapter();// Obtiene el adaptador bluetooth del celular
            }
            System.out.println("\n\tAdaptador onResume2");
            System.out.println(btAdapter);
            System.out.println(address);
            BluetoothDevice device = btAdapter.getRemoteDevice(address);// Objeto que representa al modulo bluetooth
            System.out.println("Dentro variable activa");
            try {
                btSocket = createBluetoothSocket(device);
                System.out.println("\n\tSocket encontrado");
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "La creacci??n del Socket fallo", Toast.LENGTH_LONG).show();
            }
            // Establece la conexi??n con el socket Bluetooth.
            try {
                btSocket.connect();
                setEnableControls(true);
                switchConectionButtonsEnable(ConnectedBT.CONNECTED);
                // Controles Habilitados y botones en configuraci??n de Conectado
                setEnableControls(true);
                switchConectionButtonsEnable(ConnectedBT.CONNECTED);
                // Mensaje Toast de buena coneccion
                Toast.makeText(getBaseContext(), "Coneccion Exitosa!!", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                try {
                    btSocket.close();
                    // Controles Deshabilitados y botones en configuraci??n de Desconectado
                    setEnableControls(false);
                    switchConectionButtonsEnable(ConnectedBT.DISCONNECTED);
                    // Mensaje Toast de coneccion fallida
                    Toast.makeText(getBaseContext(), "Coneccion Fallida!!", Toast.LENGTH_LONG).show();
                } catch (IOException e2) {

                }
            }

            // Se crea un hilo para la comunicaci??n bluetooth
            MyConexionBT = new ConnectedThread(btSocket);
            MyConexionBT.start();
            System.out.println("\n\tConection start");
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(btSocket!=null) {
            try {// Cuando se sale de la aplicaci??n, esto permite que no se deje abierto el socket
                btSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {

            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            //byte[] buffer = new byte[1];
            int bytes;

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    //mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    //char ch = (char) buffer[0];
                    //System.out.println("\n\t"+ch);
                    System.out.println("\n\t"+readMessage);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                    //bluetoothIn.obtainMessage(handlerState, ch).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        // Envio de trama para el arduino
        public void write(int input) {
            try {
                System.out.println("Enviado: "+input);
                mmOutStream.write(input);
                //mmOutStream.write(input.getBytes(Charset.forName("UTF-8")));
            } catch (IOException e) {
                //si no es posible enviar datos se cierra la conexi??n
                Toast.makeText(getBaseContext(), "La Conexi??n fallo", Toast.LENGTH_LONG).show();
                finish();
            }
        }

    }


}

//////////////////////////////////////////////////////////////////////////////////////////////////
/**
 * ProtectedSeekBar
 * 01/27/15 *
 * @author Jetsada Machom <jim@imjim.im>
 */
/*
public class ProtectedSeekBar extends SeekBar {
    private Drawable mThumb;

    public ProtectedSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public ProtectedSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ProtectedSeekBar(Context context) {
        super(context);
    }

    @Override
    public void setThumb(Drawable thumb) {
        super.setThumb(thumb);
        mThumb = thumb;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            if( event.getX() < mThumb.getBounds().left ||
                    event.getX() > mThumb.getBounds().right ||
                    event.getY() > mThumb.getBounds().bottom ||
                    event.getY() < mThumb.getBounds().top) {
                return false;
            }
        }
        return super.onTouchEvent(event);
    }
}
*/

//////////////////////////////////////////////////////////////////////////////////////////////////
/*
public class Slider extends SeekBar {
    private Drawable mThumb;

    public Slider(Context context) {
        super(context);
    } public Slider(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override public void setThumb(Drawable thumb) {
        super.setThumb(thumb);
        mThumb = thumb;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (event.getX() >= mThumb.getBounds().left &&
                    event.getX() <= mThumb.getBounds().right &&
                    event.getY() <= mThumb.getBounds().bottom &&
                    event.getY() >= mThumb.getBounds().top) {
                super.onTouchEvent(event);
            } else {
                return false;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            return false;
        } else {
            super.onTouchEvent(event);
        }
        return true;
    }
}
*/
/*
package com.androidbook.hiworld;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.SeekBar;
*/
//public class HiWorldActivity extends Activity {
//    int originalProgress;
    /**
     * Called when the activity is first created.
     */
    /*
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        SeekBar seek = (SeekBar)findViewById(R.id.EnableBar);
        seek.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                ((TextView)findViewById(R.id.SeekTxt)).setText("Value: "+progress);
                if(fromTouch == true){ // only allow changes by 1 up or down
                    if ((progress > (originalProgress+24)) || (progress < (originalProgress-24))) {
                        seekBar.setProgress( originalProgress);
                    } else {
                        originalProgress = progress;
                    }
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Nothing here..
            } @Override public void onStartTrackingTouch(SeekBar seekBar) {
                originalProgress = seekBar.getProgress();
            }
        });
    }
}
*/
package com.supinfo.beunreal.fragment.main;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Range;
import android.util.Size;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.viewmodel.CreationExtras;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.supinfo.beunreal.MainActivity;
import com.supinfo.beunreal.R;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * Fragment that handles the camera view. Makes use of the CameraKit library
 * in order to achieve this.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraViewFragment extends Fragment implements View.OnClickListener {

    private View view;

    public static Bitmap bitmap;
    public TextureView textureView;

    private static final int CAMERA_FRAGMENT_PERMISSIONS_CODE = 0;
    private int cameraFacing;

    private Size previewSize;
    private String cameraId;

    private TextureView.SurfaceTextureListener surfaceTextureListener;

    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback stateCallback;
    private CameraManager cameraManager;
    private CameraCaptureSession cameraCaptureSession;
    private CameraCharacteristics  mCameraCharacteristics;

    private CaptureRequest captureRequest;
    private CaptureRequest.Builder captureRequestBuilder;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private int screenWidth;
    private int screenHeight;

    private ImageButton mProfile;
    private ImageButton mFlash;


    public static CameraViewFragment newInstance() {
        return new CameraViewFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_camera_view, container, false);
        textureView = view.findViewById(R.id.textureView);

        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (String cameraId : cameraManager.getCameraIdList()) {
                    mCameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                }
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        initializeObjects();

        return view;
    }

    /**
     * Initializes the UI elements
     */
    private void initializeObjects() {
        ImageButton mReverse = view.findViewById(R.id.reverse);
        mProfile = view.findViewById(R.id.profile);
        EditText mSearch = view.findViewById(R.id.search);
        mFlash = view.findViewById(R.id.flash);

        mReverse.setOnClickListener(this);
        mProfile.setOnClickListener(this);
        mSearch.setOnClickListener(this);
        mFlash.setOnClickListener(this);

        screenWidth = getScreenSize().x;
        screenHeight = getScreenSize().y;

        requestPermissions(new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                CAMERA_FRAGMENT_PERMISSIONS_CODE);

        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        }


        surfaceTextureListener = initSurfaceTextureListener();
        stateCallback = initStateCallback();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    if (((MainActivity) getActivity()).getUser().getImage() != null)
                        Glide.with(getActivity())
                                .load(((MainActivity) getActivity()).getUser().getImage())
                                .apply(RequestOptions.circleCropTransform())
                                .into(mProfile);

                    handler.postDelayed(this, 1000);
                }

            }
        }, 1000);

    }

    private TextureView.SurfaceTextureListener initSurfaceTextureListener() {
        return new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                                  int width, int height) {
                setUpCamera(screenWidth, screenHeight);
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                    int width, int height) {
                // onSurfaceTextureSizeChanged()
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                // onSurfaceTextureUpdated()
            }


        };
    }

    private CameraDevice.StateCallback initStateCallback() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    CameraViewFragment.this.cameraDevice = cameraDevice;
                    createCameraPreviewSession();

                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    cameraDevice.close();
                    CameraViewFragment.this.cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int error) {
                    cameraDevice.close();
                    CameraViewFragment.this.cameraDevice = null;
                }
            };
        }
        return null;
    }

    protected Point getScreenSize() {
        Display display = getActivity().
                getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(getContext(), "Couldn't access camera or save picture",
                        Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        openBackgroundThread();
        startOpeningCamera();

        if (getView() != null) {
            getView().setFocusableInTouchMode(true);
            getView().requestFocus();
            getView().setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {

                    return false;
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        closeCamera();
        closeBackgroundThread();

        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
    }

    private void setUpCamera(int width, int height) {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        cameraFacing) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    previewSize = chooseOptimalSize(streamConfigurationMap
                            .getOutputSizes(SurfaceTexture.class), width, height);
                    this.cameraId = cameraId;
                    return;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            fixDarkPreview();

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();
                                CameraViewFragment.this.cameraCaptureSession = cameraCaptureSession;
                                CameraViewFragment.this.cameraCaptureSession
                                        .setRepeatingRequest(captureRequest, null, backgroundHandler);

                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            // onConfigureFailed()
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }



    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void startOpeningCamera() {
        if (textureView.isAvailable()) {
            setUpCamera(screenWidth, screenHeight);
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    private Size chooseOptimalSize(Size[] outputSizes, int width, int height) {
        double preferredRatio = height / (double) width;
        Size currentOptimalSize = outputSizes[0];
        double currentOptimalRatio = currentOptimalSize.getWidth() / (double) currentOptimalSize.getHeight();
        for (Size currentSize : outputSizes) {
            double currentRatio = currentSize.getWidth() / (double) currentSize.getHeight();
            if (Math.abs(preferredRatio - currentRatio) <
                    Math.abs(preferredRatio - currentOptimalRatio)) {
                currentOptimalSize = currentSize;
                currentOptimalRatio = currentRatio;
            }
        }
        return currentOptimalSize;
    }
    private void fixDarkPreview() throws CameraAccessException {
        Range<Integer>[] autoExposureFPSRanges = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

        if (autoExposureFPSRanges != null) {
            for (Range<Integer> autoExposureRange : autoExposureFPSRanges) {
                if (autoExposureRange.equals(Range.create(15, 30))) {
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                            Range.create(15, 30));
                }
            }
        }
    }


    public void TakePhoto(){
        bitmap = textureView.getBitmap();
        Toast.makeText(getContext(), "You're beautiful!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {

    }

    @NonNull
    @NotNull
    @Override
    public CreationExtras getDefaultViewModelCreationExtras() {
        return null;
    }
}

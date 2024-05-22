package com.supinfo.beunreal.fragment.main;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Range;
import android.util.Size;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.supinfo.beunreal.MainActivity;
import com.supinfo.beunreal.R;

import java.util.Collections;


public class Camera extends Fragment implements View.OnClickListener {

    public  TextureView textureView;
    private View view;

    private int cameraFacing;

    private Size previewSize;
    private String cameraId;

    private TextureView.SurfaceTextureListener surfaceTextureListener;

    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback stateCallback;
    private CameraManager cameraManager;
    private CameraCaptureSession cameraCaptureSession;

    private CaptureRequest captureRequest;
    private CaptureRequest.Builder captureRequestBuilder;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private int screenWidth;
    private int screenHeight;
    private ImageButton mProfile;

    public static Camera newInstance() {
        return new Camera();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screenWidth = getScreenSize().x;
        screenHeight = getScreenSize().y;
        surfaceTextureListener = initSurfaceTextureListener();
        stateCallback = initStateCallback();
    }

    /**
     * Initializes the UI elements
     */
    private void initializeObjects() {
        ImageButton mReverse = view.findViewById(R.id.reverse);
        mProfile = view.findViewById(R.id.profile);
        EditText mSearch = view.findViewById(R.id.search);
        ImageButton mFlash = view.findViewById(R.id.flash);

        mReverse.setOnClickListener(this);
        mProfile.setOnClickListener(this);
        mSearch.setOnClickListener(this);
        mFlash.setOnClickListener(this);

        screenWidth = getScreenSize().x;
        screenHeight = getScreenSize().y;

        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;
        cameraManager = (CameraManager) requireActivity().getSystemService(Context.CAMERA_SERVICE);

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
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture,
                                                  int width, int height) {
                setUpCamera(screenWidth, screenHeight);
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture,
                                                    int width, int height) {
                // onSurfaceTextureSizeChanged()
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                // onSurfaceTextureUpdated()
            }


        };
    }

    private CameraDevice.StateCallback initStateCallback() {
        return new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                Camera.this.cameraDevice = cameraDevice;
                createCameraPreviewSession();

            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                cameraDevice.close();
                Camera.this.cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int error) {
                cameraDevice.close();
                Camera.this.cameraDevice = null;
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_camera_view, container, false);
        textureView = view.findViewById(R.id.textureView);
        initializeObjects();
        try
        {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics mCameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }




        return view;
    }

    protected Point getScreenSize() {
        Display display = requireActivity().
                getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        openBackgroundThread();
        startOpeningCamera();

        if (getView() != null) {
            getView().setFocusableInTouchMode(true);
            getView().requestFocus();
            getView().setOnKeyListener((view, keyCode, keyEvent) -> false);
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
                    assert streamConfigurationMap != null;
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
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
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
            assert surfaceTexture != null;
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
                                Camera.this.cameraCaptureSession = cameraCaptureSession;
                                Camera.this.cameraCaptureSession
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
        ((MainActivity) requireActivity()).setBitmapToSend(textureView.getBitmap());
        ((MainActivity) requireActivity()).openDisplayImageFragment();
    }

    /**
     * Handles onClick events
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.profile:
                ((MainActivity) requireActivity()).openProfileEditFragment();
                break;
            case R.id.search:
                ((MainActivity) requireActivity()).openFindUsersFragment();
                break;
        }
    }




}


package com.tzxing.voicedetection.ui.record;


import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

import android.util.Log;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tzxing.voicedetection.R;
import com.tzxing.voicedetection.databinding.FragmentRecordBinding;


public class RecordFragment extends Fragment {

    private RecordViewModel recordViewModel;
    private FragmentRecordBinding binding;
    private boolean isRecording = false;
    private boolean isPlaying = false;

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;

    private MediaRecorder recorder = null;

    private MediaPlayer   player = null;

    protected CustomAdapter mAdapter;
//    protected String[] mDataset;
    protected ArrayList<File> mDataset;

    protected RecyclerView.LayoutManager mLayoutManager;

    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize dataset, this data would usually come from a local content provider or
        // remote server.
        initDataset();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recordViewModel =
                new ViewModelProvider(this).get(RecordViewModel.class);

        binding = FragmentRecordBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        requestPermissions(permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        //患者姓名
        final TextView textView = binding.editPersonName;


        //类型选择器
        final Spinner spinner = binding.typeSpinner;
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        //录音按钮
        final Button button = binding.recordButton;

        //录音结果列表
        final RecyclerView recyclerView = binding.resultList;
        mLayoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new CustomAdapter(mDataset);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(mLayoutManager);
        mAdapter.setOnItemClickListener(new CustomAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, CustomAdapter.ViewName viewName, int position) {
                switch (v.getId()){
                    case R.id.item_play_button:
                        if (!isPlaying) {
                            Toast.makeText(getActivity(),"开始播放"+mDataset.get(position).getName(),Toast.LENGTH_SHORT).show();
                            isPlaying = true;
                            int duration=startPlaying(mDataset.get(position).getAbsolutePath());
                            new Handler().postDelayed(new Runnable(){
                                public void run() {
                                    stopPlaying();
                                    isPlaying = false;
                                }
                            }, duration);
                        }else{
                            stopPlaying();
                            isPlaying = false;
                        }

                        break;
                    case R.id.item_delete_button:
                        Toast.makeText(getActivity(),"已删除"+mDataset.get(position).getName(),Toast.LENGTH_SHORT).show();
                        deleteFile(mDataset.get(position).getAbsolutePath());
                        updateDataset();
                        mAdapter.notifyDataSetChanged();
                        break;
                    default:
                        Toast.makeText(getActivity(),"你点击了item"+(position+1),Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onItemLongClick(View v) {

            }
        });

        button.setOnClickListener(v -> {
            fileName = getActivity().getExternalCacheDir().getAbsolutePath();
            fileName += "/"+textView.getText()+"_"+spinner.getSelectedItem().toString()+"_"+ DateFormat.format("yyyyMMdd_HHmmss", Calendar.getInstance(Locale.CHINA))+".3gp";
            onRecord(!isRecording);
            if (!isRecording) {
                Toast.makeText(button.getContext(), "开始录制", Toast.LENGTH_SHORT).show();
                button.setText("停止");
            } else {
                Toast.makeText(button.getContext(), "结束录制", Toast.LENGTH_SHORT).show();
                button.setText("录音");
                updateDataset();
                mAdapter.notifyDataSetChanged();
            }
            isRecording = !isRecording;
        });
        return root;
    }

    private void updateDataset() {
        File file = new File(getActivity().getExternalCacheDir().getAbsolutePath());
        File[] files = file.listFiles();
//        if (files == null) {
//            mDataset = new ArrayList<>();
//        }
//        mDataset = new ArrayList<>();
        mDataset.clear();
        for (int i = 0; i < files.length; ++i) {
            mDataset.add(files[i]) ;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted )
            getActivity().finish();

    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

//    private void onPlay(boolean start,String fileName) {
//        if (start) {
//            startPlaying(fileName);
//        } else {
//            stopPlaying();
//        }
//    }

    private int startPlaying(String fileName) {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        return player.getDuration();
    }

    private void stopPlaying() {
        if (player == null) {
            return;
        }
        player.release();
        player = null;
        Log.i("STOP", "tzbf");
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void initDataset() {
        File file = new File(getActivity().getExternalCacheDir().getAbsolutePath());
        File[] files = file.listFiles();
//        if (files == null) {
//            mDataset = new ArrayList<>();
//        }
        mDataset = new ArrayList<>();
        for (int i = 0; i < files.length; ++i) {
            mDataset.add(files[i]) ;
        }
    }
    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }
}
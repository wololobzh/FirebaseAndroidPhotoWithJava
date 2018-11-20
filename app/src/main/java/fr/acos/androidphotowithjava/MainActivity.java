package fr.acos.androidphotowithjava;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private StorageReference stockageCloud;
    Uri downloadUrl=null;
    String localImageFilePath;

    private static final int REQUEST_IMAGE_CAPTURE=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.stockageCloud = FirebaseStorage.getInstance().getReference();
    }


    public void prendrePhoto(View view){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photo = createImageFile();
            Uri uriToUpload = FileProvider.getUriForFile(MainActivity.this,
                    "fr.eni.projetphoto.provider",
                    photo);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriToUpload);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode)
        {
            case REQUEST_IMAGE_CAPTURE:
                Bitmap imageBitmap = BitmapFactory.decodeFile(this.localImageFilePath);
                ImageView iv = findViewById(R.id.ivPhotoPrise);
                iv.setImageBitmap(imageBitmap);
                break;
            default:
                super.onActivityResult(requestCode,resultCode,data);
        }
    }

    public void uploadImage(View view) {
        StorageReference cloudRef = this.stockageCloud.child(Paths.get(localImageFilePath).getFileName().toString());
        cloudRef.putFile(Uri.fromFile(new File(localImageFilePath)))
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        downloadUrl = taskSnapshot.getDownloadUrl();
                        Log.i("XXX", downloadUrl.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // ...
                    }
                });
    }

    public void downloadImage(View view) {
        StorageReference httpsReference = this.stockageCloud.getStorage().getReferenceFromUrl(downloadUrl.toString());
        try {
            final File localFile = File.createTempFile("images", "jpg");

            httpsReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    ImageView iv = findViewById(R.id.ivPhotoTelechargee);
                    try {
                        iv.setImageBitmap(BitmapFactory.decodeStream(new FileInputStream(localFile)));
                        Log.i("XXX", "Chargement OK");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.e("XXX", "Chargement KO : " + exception.toString());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private File createImageFile()  {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss",
                        Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir =
                getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            localImageFilePath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
}

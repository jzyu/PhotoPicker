package me.iwf.photopicker.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by donglua on 15/6/23.
 *
 *
 * http://developer.android.com/training/camera/photobasics.html
 */
public class ImageCaptureManager {

  private final static String CAPTURED_PHOTO_PATH_KEY = "mCurrentPhotoPath";
  public static final int REQUEST_TAKE_PHOTO = 1;

  private String mCurrentPhotoPath;
  private Context mContext;

  public ImageCaptureManager(Context mContext) {
    this.mContext = mContext;
  }

  private File createImageFile(String privateDir) throws IOException {
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";

    File storageDir;
    if (privateDir != null && privateDir.length() > 0) {
      File baseDir = Environment.getExternalStorageDirectory();
      storageDir = new File(baseDir.getAbsolutePath() + File.separator + privateDir);
    } else {
      storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    }
    if (!storageDir.exists()) {
      if (!storageDir.mkdir()) {
        throw new IOException();
      }
    }
    File image = File.createTempFile(
        imageFileName,  /* prefix */
        ".jpg",         /* suffix */
        storageDir      /* directory */
    );

    // Save a file: path for use with ACTION_VIEW intents
    mCurrentPhotoPath = image.getAbsolutePath();
    return image;
  }


  public Intent dispatchTakePictureIntent(String privateDir) throws IOException {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Ensure that there's a camera activity to handle the intent
    if (takePictureIntent.resolveActivity(mContext.getPackageManager()) != null) {
      // Create the File where the photo should go
      File photoFile = createImageFile(privateDir);
      // Continue only if the File was successfully created
      if (photoFile != null) {
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
            Uri.fromFile(photoFile));
      }
    }
    return takePictureIntent;
  }


  public void galleryAddPic() {
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    File f = new File(mCurrentPhotoPath);
    Uri contentUri = Uri.fromFile(f);
    mediaScanIntent.setData(contentUri);
    mContext.sendBroadcast(mediaScanIntent);
  }


  public String getCurrentPhotoPath() {
    return mCurrentPhotoPath;
  }


  public void onSaveInstanceState(Bundle savedInstanceState) {
    if (savedInstanceState != null && mCurrentPhotoPath != null) {
      savedInstanceState.putString(CAPTURED_PHOTO_PATH_KEY, mCurrentPhotoPath);
    }
  }

  public void onRestoreInstanceState(Bundle savedInstanceState) {
    if (savedInstanceState != null && savedInstanceState.containsKey(CAPTURED_PHOTO_PATH_KEY)) {
      mCurrentPhotoPath = savedInstanceState.getString(CAPTURED_PHOTO_PATH_KEY);
    }
  }

  public boolean resizePhoto(final int destWidth) {
    // original measurements
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(mCurrentPhotoPath, options);

    int origWidth = options.outWidth;
    int origHeight = options.outHeight;

    if (origWidth <= destWidth)
      return true;

    // picture is wider than we want it, we calculate its target height
    int destHeight = origHeight / (origWidth / destWidth);

    options = new BitmapFactory.Options();
    options.inJustDecodeBounds = false;
    options.inPreferredConfig = Bitmap.Config.RGB_565;
    options.inDither = true;
    Bitmap b = BitmapFactory.decodeFile(mCurrentPhotoPath, options);

    // we create an scaled bitmap so it reduces the image, not just trim it
    Bitmap b2 = Bitmap.createScaledBitmap(b, destWidth, destHeight, false);
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    // compress to the format you want, JPEG, PNG...
    // 70 is the 0-100 quality percentage
    b2.compress(Bitmap.CompressFormat.JPEG, 70, outStream);

    // we save the file, at least until we have made use of it
    File f = new File(mCurrentPhotoPath + ".resized");

    try {
      if (! f.createNewFile())
        return false;

      FileOutputStream fo = new FileOutputStream(f);
      fo.write(outStream.toByteArray());
      fo.close();

      return f.renameTo(new File(mCurrentPhotoPath));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }
}

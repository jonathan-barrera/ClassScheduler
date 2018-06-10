package com.example.android.classscheduler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android.classscheduler.Model.Student;
import com.example.android.classscheduler.data.StudentContract.StudentEntry;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class EditStudentInfo extends AppCompatActivity {

    // Integer variable to keep track of the student's sex. Put male as the default.
    private int mStudentSex = StudentEntry.SEX_MALE;

    // Variable for current student (if editing a student's information)
    private Student mCurrentStudent;

    // Boolean variable to keep track of whether user is editing a student or adding a new student
    private boolean isEditStudent;

    // Boolean variable to keep track of whether there is a photo or not
    private boolean studentHasPhoto;

    // Photo capture/selection function constants and variables
    private static final int PICK_PHOTO_GALLERY = 1;
    private static final int CAPTURE_PHOTO = 2;
    private Bitmap mBitmap;
    private Uri mFirebaseStoragePhotoUri;
    private String mTempPhotoPath;
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.classscheduler.fileprovider";

    // Boolean to keep track of changes made to student's info
    private boolean mChangesMade = false;

    // Constant variables
    private final static String SEX_MALE = "Male";
    private final static String SEX_FEMALE = "Female";

    // Firebase Instances
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStudentPhotosStorageReference;

    // Views
    @BindView(R.id.student_name_edit_text)
    EditText mStudentNameEditText;
    @BindView(R.id.student_sex_spinner)
    Spinner mStudentSexSpinner;
    @BindView(R.id.student_age_edit_text)
    EditText mStudentAgeEditText;
    @BindView(R.id.student_grade_edit_text)
    EditText mStudentGradeEditText;
    @BindView(R.id.student_classes_edit_text)
    EditText mStudentClassesEditText;
    @BindView(R.id.gray_circle_background_view)
    ImageView mAddPhotoView;
    @BindView(R.id.add_photo_label_text_view)
    TextView mAddPhotoLabelTextView;
    @BindView(R.id.add_sign_image_view)
    ImageView mAddPhotoImageView;
    @BindView(R.id.saving_student_progress_bar)
    ProgressBar mSavingProgressBar;

    // OnTouchListener to listen for user touches on Edit Text views. A touch indicates that
    // a change has probably been made to the info.
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mChangesMade = true;
            return false;
        }
    };

    // Listener for text changes (used on the edit text views)
    private TextWatcher mTextListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mChangesMade = true;
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_student_info);

        // Bind the views
        ButterKnife.bind(this);

        // Initialize Firebase references
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference().child("students");
        mFirebaseStorage = FirebaseStorage.getInstance();
        mStudentPhotosStorageReference = mFirebaseStorage.getReference().child("student_photos");

        // Get intent to see whether we are updating an old student or adding a new student
        mCurrentStudent = getIntent().getParcelableExtra(StudentListActivity.STUDENT_EXTRA_KEY);

        if (mCurrentStudent != null) {
            setTitle("Edit Profile");
            isEditStudent = true;
            fillInCurrentStudentInfo();
        } else {
            setTitle("Add Student");
        }

        // Set up the spinner
        setUpSpinner();

        // Set touch listeners
        setTouchListeners();

        // Set Add Photo OnClickListener to allow user to choose a photo or take one
        setUpAddPhoto();

        // Check for student photo
        checkIfThereIsStudentPhoto();
    }

    // Method for filling in a current student's information into the input fields
    private void fillInCurrentStudentInfo() {
        // Extract info from Student object
        String name = mCurrentStudent.getName();
        int sex = mCurrentStudent.getSex();
        int age = mCurrentStudent.getAge();
        int grade = mCurrentStudent.getGrade();
        String classes = mCurrentStudent.getClasses();
        String photoUrl = mCurrentStudent.getPhotoUrl();

        // Populate views with the current student's information
        mStudentNameEditText.setText(name);
        mStudentAgeEditText.setText(String.valueOf(age));
        mStudentGradeEditText.setText(String.valueOf(grade));
        mStudentClassesEditText.setText(classes);

        // Set student photo (if it exists)
        if (!TextUtils.isEmpty(photoUrl)) {
            Glide.with(mAddPhotoView.getContext())
                    .load(photoUrl)
                    .into(mAddPhotoView);
            studentHasPhoto = true;
        }

        // Set student sex spinner
        switch (sex) {
            case StudentEntry.SEX_MALE:
                mStudentSexSpinner.setSelection(StudentEntry.SEX_MALE);
                break;
            case StudentEntry.SEX_FEMALE:
                mStudentSexSpinner.setSelection(StudentEntry.SEX_FEMALE);
                break;
            default:
                throw new IllegalArgumentException("Invalid sex");
        }
    }

    // Set up OnClickListener for the AddPhoto imageview
    @SuppressLint("RestrictedApi")
    private void setUpAddPhoto() {
        mAddPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuBuilder menuBuilder = new MenuBuilder(EditStudentInfo.this);
                new MenuInflater(EditStudentInfo.this).inflate(R.menu.menu_photo, menuBuilder);
                MenuPopupHelper menuPopupHelper = new MenuPopupHelper(EditStudentInfo.this,
                        menuBuilder, mAddPhotoView);
                menuPopupHelper.setForceShowIcon(true);
                menuBuilder.setCallback(new MenuBuilder.Callback() {
                    // Create a menu for the user to choose between taking a new photo and
                    // choosing a photo from their photo gallery
                    @Override
                    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.photo_gallery_action:
                                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                                galleryIntent.setType("image/*");
                                startActivityForResult(galleryIntent, PICK_PHOTO_GALLERY);
                                break;
                            case R.id.camera_action:
                                launchCamera();
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid menu item selected");
                        }
                        return true;
                    }
                    @Override
                    public void onMenuModeChange(MenuBuilder menu) {

                    }
                });
                menuPopupHelper.show();
            }
        });
    }

    // Helper method to check if the student profile has a photo or not.
    // If so, hide the "+" and "add photo" views
    private void checkIfThereIsStudentPhoto() {
        if (studentHasPhoto) {
            mAddPhotoImageView.setVisibility(View.GONE);
            mAddPhotoLabelTextView.setVisibility(View.GONE);
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Check for camera on phone
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Temporary file to hold image
            File photoFile = null;
            try {
                photoFile = BitmapUtils.createTempImageFile(this);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (photoFile != null) {
                // Get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();

                // Get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);

                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
                startActivityForResult(takePictureIntent, CAPTURE_PHOTO);
            }
        }
    }

    // Override onActivityResult to work with the photo that the user has captured/selected
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Pick photo
        if (requestCode == PICK_PHOTO_GALLERY && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(this, "Error selecting photo.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                mBitmap = BitmapFactory.decodeStream(inputStream);
                mAddPhotoView.setImageBitmap(mBitmap);
                studentHasPhoto = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            // Take new photo
        } else if (requestCode == CAPTURE_PHOTO && resultCode == Activity.RESULT_OK) {
            mBitmap = BitmapFactory.decodeFile(mTempPhotoPath);
            mAddPhotoView.setImageBitmap(mBitmap);
            studentHasPhoto = true;
        }
        checkIfThereIsStudentPhoto();
    }

    // Set listeners on the edit text views and spinner
    private void setTouchListeners() {
        mStudentNameEditText.addTextChangedListener(mTextListener);
        mStudentSexSpinner.setOnTouchListener(mTouchListener);
        mStudentAgeEditText.addTextChangedListener(mTextListener);
        mStudentGradeEditText.addTextChangedListener(mTextListener);
        mStudentClassesEditText.addTextChangedListener(mTextListener);
        mAddPhotoView.setOnTouchListener(mTouchListener);
    }

    // Helper method for setting up the spinner for choosing a student's sex
    private void setUpSpinner() {
        // Create adapter for student sex spinner
        ArrayAdapter sexSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, R.layout.sex_spinner_item);

        // Specify dropdown layout style
        sexSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply adapter to spinner
        mStudentSexSpinner.setAdapter(sexSpinnerAdapter);

        // Set an onitemseledctedlistener onto the spinner
        mStudentSexSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                switch (selection) {
                    case SEX_MALE:
                        mStudentSex = StudentEntry.SEX_MALE;
                        break;
                    case SEX_FEMALE:
                        mStudentSex = StudentEntry.SEX_FEMALE;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid Student Sex");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Male is already set as default
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the edit student menu
        getMenuInflater().inflate(R.menu.edit_student_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                // Save student's information
                saveStudent();
                return true;
            case android.R.id.home:
                // Check if changes have been made
                if (!mChangesMade) {
                    if (isEditStudent) {
                        onBackPressed();
                    } else {
                        NavUtils.navigateUpFromSameTask(this);
                    }
                    return true;
                } else {
                    showUnsavedChangesDialog();
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    // Helper method for saving student information to database
    private void saveStudent() {
        // Check that every required field has been filled in with valid parameters
        if (!checkUserInputValidity()) {
            return;
        }

        // Insert the new student info into the database
        if (isEditStudent) {
            updateStudentOnFirebaseDatabase();
        } else {
            // Make a unique id for the student
            String studentId = UUID.randomUUID().toString();
            saveNewStudentToFirebaseDatabase(studentId);
        }
    }

    // Save the student photo to the Firebase Storage when the Student info is saved
    private void saveStudentPhotoToFirebaseStorage(final String studentId) {
        // Show progress bar
        mSavingProgressBar.setVisibility(View.VISIBLE);

        // Save photo to firebase storage
        final StorageReference photoRef = mStudentPhotosStorageReference.child(studentId);
        photoRef.putBytes(BitmapUtils.bitmapToByteArray(mBitmap)).addOnSuccessListener(
                new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        // On success, get the URL link, which will be saved in the student database
                        mFirebaseStoragePhotoUri = uri;

                        String studentPhoto = mFirebaseStoragePhotoUri.toString();
                        String studentName = mStudentNameEditText.getText().toString().trim();
                        int studentSex = mStudentSex;
                        int studentAge = Integer.parseInt(mStudentAgeEditText.getText().toString());
                        int studentGrade = Integer.parseInt(mStudentGradeEditText.getText().toString());
                        String studentClasses = mStudentClassesEditText.getText().toString().trim();

                        Student newStudent = new Student(studentName, studentSex, studentAge, studentGrade,
                                studentClasses, studentPhoto, studentId);

                        mDatabaseReference.child(studentId).setValue(newStudent);

                        finish();
                    }
                });
            }
        });
    }

    // Method to save a new student to the Firebase Database
    private void saveNewStudentToFirebaseDatabase(String studentId) {
        // Extract Student information from the edit text views
        String studentName = mStudentNameEditText.getText().toString().trim();
        int studentSex = mStudentSex;
        int studentAge = Integer.parseInt(mStudentAgeEditText.getText().toString());
        int studentGrade = Integer.parseInt(mStudentGradeEditText.getText().toString());
        String studentClasses = mStudentClassesEditText.getText().toString().trim();

        if (mBitmap != null) {
            // Save photo to Firebase Storage using AsyncTask
            saveStudentPhotoToFirebaseStorage(studentId);
        } else {
            Student newStudent = new Student(studentName, studentSex, studentAge, studentGrade,
                    studentClasses, null, studentId);

            mDatabaseReference.child(studentId).setValue(newStudent);
        }

        // Close activity
        finish();
    }

    // Update existing student's information on the Firebase Database
    private void updateStudentOnFirebaseDatabase() {
        String studentName = mStudentNameEditText.getText().toString().trim();
        int studentSex = mStudentSex;
        int studentAge = Integer.parseInt(mStudentAgeEditText.getText().toString());
        int studentGrade = Integer.parseInt(mStudentGradeEditText.getText().toString());
        String studentClasses = mStudentClassesEditText.getText().toString().trim();
        String studentId = mCurrentStudent.getStudentId();

        if (mBitmap != null) {
            saveStudentPhotoToFirebaseStorage(studentId);
        } else {
            // Check if the student already has a photo saved
            String photoUrl;
            if (studentHasPhoto) {
                // if has photo, resave the photo to the student database
                photoUrl = mCurrentStudent.getPhotoUrl();
            } else {
                photoUrl = null;
            }

            mFirebaseDatabase.getReference().child("students").child(studentId)
                    .setValue(new Student(studentName, studentSex, studentAge, studentGrade,
                            studentClasses, photoUrl, studentId));

            // Close activity
            finish();
        }
    }

    // Helper method to check that the user's input for each field is valid
    private boolean checkUserInputValidity() {
        // Extract Student information from the edit text views
        String studentName = mStudentNameEditText.getText().toString().trim();
        String studentAgeString = mStudentAgeEditText.getText().toString();
        String studentGradeString = mStudentGradeEditText.getText().toString();

        // Check for valid student name
        if (TextUtils.isEmpty(studentName)) {
            Toast.makeText(this, "Please enter a valid name.", Toast.LENGTH_SHORT).show();
            mStudentNameEditText.requestFocus();
            return false;
        }

        // Check for valid student age
        if (TextUtils.isEmpty(studentAgeString)) {
            Toast.makeText(this, "Please enter a valid age.", Toast.LENGTH_SHORT).show();
            mStudentAgeEditText.requestFocus();
            return false;
        } else {
            int studentAge = Integer.parseInt(studentAgeString);
            if (studentAge <= 0) {
                Toast.makeText(this, "Please enter a valid age.", Toast.LENGTH_SHORT).show();
                mStudentAgeEditText.requestFocus();
                return false;
            }
        }

        // Check for valid student grade
        if (TextUtils.isEmpty(studentGradeString)) {
            Toast.makeText(this, "Please enter a valid grade.", Toast.LENGTH_SHORT).show();
            mStudentGradeEditText.requestFocus();
            return false;
        } else {
            int studentGrade = Integer.parseInt(studentGradeString);
            if (studentGrade <= 0) {
                Toast.makeText(this, "Please enter a valid grade.", Toast.LENGTH_SHORT).show();
                mStudentGradeEditText.requestFocus();
                return false;
            }
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        Timber.d("onbackpressed called " + mChangesMade);
        if (!mChangesMade) {
            super.onBackPressed();
            return;
        }

        // If there are any unsaved changes, show dialog to warn the user
        showUnsavedChangesDialog();
    }

    // Inform the user that there are unsaved changes to the student's information
    private void showUnsavedChangesDialog() {
        Timber.d("showUnsavedChangesDialog called");
        // Create an AlertDialog.Builder and set the message and click listeners for the positive
        // and negative buttons.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You have unsaved changes. Would you like to leave this page?");

        // Leave the page if user clicks "Leave"
        builder.setPositiveButton("Leave", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        // Stay on the page if user clicks "Stay"
        builder.setNegativeButton("Stay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}
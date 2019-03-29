package com.storyline;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.storyline.ui.FullStoryDialogFragment;
import com.storyline.ui.categories.model.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatFragment extends Fragment {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messageImageView = (ImageView) itemView.findViewById(R.id.messageImageView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }

    private static final String TAG = "MainActivity";
    public String MESSAGES_CHILD = "";

    private static final int STORY_LENGTH = 5;

    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private String mUsername;
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;
    private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";

    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;
    private TextView textViewOpen;
    private FullStoryDialogFragment fullStoryDialogFragment;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<FriendlyMessage, MainActivity.MessageViewHolder>
            mFirebaseAdapter;

    private String openingSentance;
    private String friendUserId;
    private Category category;

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setOpeningSentance(String openingSentance) {
        this.openingSentance = openingSentance;
    }

    public void setMESSAGES_CHILD(String MESSAGES_CHILD) {
        this.MESSAGES_CHILD = MESSAGES_CHILD;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.activity_main, container, false);
    }

    private void setFriendUserId(){
        friendUserId = MESSAGES_CHILD;
        friendUserId =  friendUserId.replace(mFirebaseUser.getUid(),"");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize ProgressBar and RecyclerView.
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        setFriendUserId();
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) view.findViewById(R.id.messageRecyclerView);
        textViewOpen = view.findViewById(R.id.textViewOpen);

        textViewOpen.setText(openingSentance);

        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        // New child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();



//        mFirebaseDatabaseReference.child(friendUserId).child("interActiveFriendList").child(mFirebaseUser.getUid()).setValue(new InterActiveFriend(mFirebaseUser.getUid(),true,lastword));


        mFirebaseDatabaseReference.child("users").child(friendUserId).child("interActiveFriendList").child(mFirebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                InterActiveFriend interActiveFriend = snapshot.getValue(InterActiveFriend.class);
                if(interActiveFriend != null && !TextUtils.isEmpty(interActiveFriend.lastWord)){
                    textViewOpen.setText("Your line starts with the word " + interActiveFriend.lastWord);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });










        SnapshotParser<FriendlyMessage> parser = new SnapshotParser<FriendlyMessage>() {
            @Override
            public FriendlyMessage parseSnapshot(DataSnapshot dataSnapshot) {
                FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                if (friendlyMessage != null) {
                    friendlyMessage.setId(dataSnapshot.getKey());
                }
                return friendlyMessage;
            }
        };

        DatabaseReference messagesRef = mFirebaseDatabaseReference.child(MESSAGES_CHILD);
        FirebaseRecyclerOptions<FriendlyMessage> options =
                new FirebaseRecyclerOptions.Builder<FriendlyMessage>()
                        .setQuery(messagesRef, parser)
                        .build();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<FriendlyMessage, MainActivity.MessageViewHolder>(options) {

            @Override
            public MainActivity.MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MainActivity.MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup,
                        false));
            }

            @Override
            protected void onBindViewHolder(final MainActivity.MessageViewHolder viewHolder,
                                            int position,
                                            FriendlyMessage friendlyMessage) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (friendlyMessage.getText() != null) {
                    viewHolder.messageTextView.setText(friendlyMessage.getText());
                    viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                    viewHolder.messageImageView.setVisibility(ImageView.GONE);
                } else if (friendlyMessage.getImageUrl() != null) {
                    String imageUrl = friendlyMessage.getImageUrl();
                    if (imageUrl.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            Glide.with(viewHolder.messageImageView.getContext())
                                                    .load(downloadUrl)
                                                    .into(viewHolder.messageImageView);
                                        } else {
                                            Log.w(TAG, "Getting download url was not successful.",
                                                    task.getException());
                                        }
                                    }
                                });
                    } else {
                        Glide.with(viewHolder.messageImageView.getContext())
                                .load(friendlyMessage.getImageUrl())
                                .into(viewHolder.messageImageView);
                    }
                    viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                    viewHolder.messageTextView.setVisibility(TextView.GONE);
                }


                viewHolder.messengerTextView.setText(friendlyMessage.getName());
                if (friendlyMessage.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(getActivity())
                            .load(friendlyMessage.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }

            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);


        mMessageEditText = (EditText) view.findViewById(R.id.messageEditText);
//        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
//                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) view.findViewById(R.id.sendButton);
//        mSendButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // Send messages on click.
//            }
//        });

//        mAddMessageImageView = (ImageView) view.findViewById(R.id.addMessageImageView);
//        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // Select image for image message on click.
//            }
//        });

        // Initialize Firebase Auth

//        if (mFirebaseUser == null) {
//            // Not signed in, launch the Sign In activity
//            startActivity(new Intent(this, SignInActivityWithUsernameAndPassword.class));
//            finish();
//            return;
//        } else {
//            mUsername = mFirebaseUser.getDisplayName();
//            if (mFirebaseUser.getPhotoUrl() != null) {
//                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
//            }
//        }

        mUsername = mFirebaseUser.getDisplayName();
        if (mFirebaseUser.getPhotoUrl() != null) {
            mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
        }

        mSendButton = (Button) view.findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            //    if(mFirebaseAdapter.getItemCount() < STORY_LENGTH) {

                boolean isLastTurn = false;
                    if (mFirebaseAdapter.getItemCount() < STORY_LENGTH - 1) {
                        ((MainStoriesActivity) getActivity()).moveToFriendsListFragment();
                    } else {

                        Toast.makeText(getActivity(), "end game", Toast.LENGTH_LONG).show();

                        StringBuilder fullStory = new StringBuilder();
                        for(int i = 0; i< mFirebaseAdapter.getItemCount() - 1; ++i){

                            fullStory.append(mFirebaseAdapter.getItem(i).getText().trim()).append("\n");

                        }
                        fullStory.append(mMessageEditText.getText().toString().trim());
                        Toast.makeText(getActivity(), fullStory.toString(), Toast.LENGTH_LONG).show();
                        fullStoryDialogFragment = FullStoryDialogFragment.newInstance(fullStory.toString());
                        fullStoryDialogFragment.show(getChildFragmentManager(), null);

                        final DatabaseReference updateTurnsReferance = mFirebaseDatabaseReference.child("users");

                        String lastword = mMessageEditText.getText().toString().trim().substring(mMessageEditText.getText().toString().lastIndexOf(" ") + 1);

                        //update friends turn
                        updateTurnsReferance.child(friendUserId).child("interActiveFriendList").child(mFirebaseUser.getUid()).setValue(new InterActiveFriend(mFirebaseUser.getUid(), InterActiveFriend.FRIEND_TURN, lastword));

                        //update my turn
                        updateTurnsReferance.child(mFirebaseUser.getUid()).child("interActiveFriendList").child(friendUserId).setValue(new InterActiveFriend(friendUserId, InterActiveFriend.END_GAME, lastword));

                        mMessageEditText.setText("");

                        isLastTurn = true;

                    }

                    FriendlyMessage friendlyMessage = new
                            FriendlyMessage(mMessageEditText.getText().toString().trim(),
                            "mUsername",
                            mPhotoUrl,
                            null /* no image */);
                    friendlyMessage.setId(mFirebaseUser.getUid());
//                String friendUserId = MESSAGES_CHILD;
//                friendUserId =  friendUserId.replace(mFirebaseUser.getUid(),"");

                    if (!TextUtils.isEmpty(openingSentance)) {
                        FriendlyMessage friendlyMessageOpening = new
                                FriendlyMessage(openingSentance,
                                "mUsername",
                                mPhotoUrl,
                                null /* no image */);
                        friendlyMessageOpening.setId(friendUserId);
                        mFirebaseDatabaseReference.child(MESSAGES_CHILD)
                                .push().setValue(friendlyMessageOpening);
                        openingSentance = "";
                    }


                    mFirebaseDatabaseReference.child(MESSAGES_CHILD)
                            .push().setValue(friendlyMessage);


                    if (!isLastTurn) {
                        final DatabaseReference updateTurnsReferance = mFirebaseDatabaseReference.child("users");

                        String lastword = mMessageEditText.getText().toString().trim().substring(mMessageEditText.getText().toString().lastIndexOf(" ") + 1);

                        //update friends turn
                        updateTurnsReferance.child(friendUserId).child("interActiveFriendList").child(mFirebaseUser.getUid()).setValue(new InterActiveFriend(mFirebaseUser.getUid(), InterActiveFriend.FRIEND_TURN, lastword));

                        //update my turn
                        updateTurnsReferance.child(mFirebaseUser.getUid()).child("interActiveFriendList").child(friendUserId).setValue(new InterActiveFriend(friendUserId, InterActiveFriend.MY_TURN, lastword));

                        mMessageEditText.setText("");
                    }
//                }else{
//                    StringBuilder fullStory = new StringBuilder();
//                    for(int i = 0; i< mFirebaseAdapter.getItemCount() - 1; ++i){
//
//                        fullStory.append(mFirebaseAdapter.getItem(i).getText().trim()).append("\n");
//
//                    }
//                    fullStory.append(mMessageEditText.getText().toString().trim());
//                    Toast.makeText(getActivity(), fullStory.toString(), Toast.LENGTH_LONG).show();
//                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    @Override
    public void onPause() {
        mFirebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}

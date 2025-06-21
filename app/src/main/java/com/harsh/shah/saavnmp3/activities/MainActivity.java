package com.harsh.shah.saavnmp3.activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.harsh.shah.saavnmp3.ApplicationClass;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.adapters.ActivityMainAlbumItemAdapter;
import com.harsh.shah.saavnmp3.adapters.ActivityMainArtistsItemAdapter;
import com.harsh.shah.saavnmp3.adapters.ActivityMainPlaylistAdapter;
import com.harsh.shah.saavnmp3.adapters.ActivityMainPopularSongs;
import com.harsh.shah.saavnmp3.adapters.SavedLibrariesAdapter;
import com.harsh.shah.saavnmp3.databinding.ActivityMainBinding;
import com.harsh.shah.saavnmp3.fragments.HomeFragment;
import com.harsh.shah.saavnmp3.fragments.LocalMusicFragment;
import com.harsh.shah.saavnmp3.model.AlbumItem;
import com.harsh.shah.saavnmp3.network.ApiManager;
import com.harsh.shah.saavnmp3.network.NetworkChangeReceiver;
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork;
import com.harsh.shah.saavnmp3.records.AlbumsSearch;
import com.harsh.shah.saavnmp3.records.ArtistsSearch;
import com.harsh.shah.saavnmp3.records.PlaylistsSearch;
import com.harsh.shah.saavnmp3.records.SongSearch;
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries;
import com.harsh.shah.saavnmp3.utils.NetworkUtil;
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager;
import com.squareup.picasso.Picasso;
import com.yarolegovich.slidingrootnav.SlidingRootNav;
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String[]> requestStoragePermission;
    private final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private ApplicationClass applicationClass;
    final List<AlbumItem> songs = new ArrayList<>();
    final List<ArtistsSearch.Data.Results> artists = new ArrayList<>();
    final List<AlbumItem> albums = new ArrayList<>();
    final List<AlbumItem> playlists = new ArrayList<>();

    NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver(new NetworkChangeReceiver.NetworkStatusListener() {
        @Override
        public void onNetworkConnected() {
            if (songs.isEmpty() || artists.isEmpty() || albums.isEmpty() || playlists.isEmpty())
                showData();
        }

        @Override
        public void onNetworkDisconnected() {
            if (songs.isEmpty() || artists.isEmpty() || albums.isEmpty() || playlists.isEmpty())
                showOfflineData();
            Snackbar.make(binding.getRoot(), "No Internet Connection", Snackbar.LENGTH_LONG).show();
        }
    });

    public static int calculateNoOfColumns(Context context, float columnWidthDp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (screenWidthDp / columnWidthDp + 0.5);
    }

    private SlidingRootNav slidingRootNavBuilder;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applicationClass = (ApplicationClass) getApplicationContext();
        ApplicationClass.setCurrentActivity(this);
        ApplicationClass.updateTheme();

        slidingRootNavBuilder = new SlidingRootNavBuilder(this)
                .withMenuLayout(R.layout.main_drawer_layout)
                .withContentClickableWhenMenuOpened(false)
                .withDragDistance(250)
                .inject();

        // Setup bottom navigation
        setupBottomNavigation();

        // Set version text in the drawer layout
        updateVersionTextInDrawer();

        onDrawerItemsClicked();

        binding.profileIcon.setOnClickListener(view -> slidingRootNavBuilder.openMenu(true));

        int span = calculateNoOfColumns(this, 200);
        binding.playlistRecyclerView.setLayoutManager(new GridLayoutManager(this, span));

        binding.popularSongsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.popularArtistsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.popularAlbumsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.savedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        OverScrollDecoratorHelper.setUpOverScroll(binding.popularSongsRecyclerView, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL);
        OverScrollDecoratorHelper.setUpOverScroll(binding.popularArtistsRecyclerView, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL);
        OverScrollDecoratorHelper.setUpOverScroll(binding.popularAlbumsRecyclerView, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL);
        OverScrollDecoratorHelper.setUpOverScroll(binding.savedRecyclerView, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL);

        binding.refreshLayout.setOnRefreshListener(() -> {
            showShimmerData();
            showData();
            binding.refreshLayout.setRefreshing(false);
        });

        binding.playBarPlayPauseIcon.setOnClickListener(view -> {
            ApplicationClass applicationClass = (ApplicationClass) getApplicationContext();
            applicationClass.togglePlayPause();
            applicationClass.showNotification(ApplicationClass.player.isPlaying() ? R.drawable.baseline_pause_24 : R.drawable.play_arrow_24px);
            binding.playBarPlayPauseIcon.setImageResource(ApplicationClass.player.isPlaying() ? R.drawable.baseline_pause_24 : R.drawable.play_arrow_24px);
        });

        binding.playBarBackground.setOnClickListener(this::onClick);

        binding.playBarPrevIcon.setOnClickListener(view -> {
            try {
                Log.i(TAG, "Play bar previous button clicked");
                if (applicationClass == null) {
                    Log.e(TAG, "Application class is null");
                    return;
                }

                binding.playBarPrevIcon.setAlpha(0.5f);
                binding.playBarPrevIcon.animate().alpha(1.0f).setDuration(200).start();

                applicationClass.prevTrack();

                updatePlaybarUi();
            } catch (Exception e) {
                Log.e(TAG, "Error handling previous button click", e);
            }
        });

        binding.playBarNextIcon.setOnClickListener(view -> {
            try {
                Log.i(TAG, "Play bar next button clicked");
                if (applicationClass == null) {
                    Log.e(TAG, "Application class is null");
                    return;
                }

                binding.playBarNextIcon.setAlpha(0.5f);
                binding.playBarNextIcon.animate().alpha(1.0f).setDuration(200).start();

                applicationClass.nextTrack();

                updatePlaybarUi();
            } catch (Exception e) {
                Log.e(TAG, "Error handling next button click", e);
            }
        });

        showShimmerData();
        showOfflineData();

        showPlayBarData();
        showSavedLibrariesData();

        ActivityResultLauncher<Intent> requestManageAllFiles = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Toast.makeText(this, "Manage All Files Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Manage All Files Permission Denied", Toast.LENGTH_SHORT).show();
            }
        });

        requestStoragePermission = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (result.containsValue(false)) {
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            }
        });

        getStoragePermission();
    }

    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.local) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LocalMusicFragment())
                        .commit();
                return true;
            }
            // Agrega más casos para otros ítems del menú si es necesario
            return false;
        });

        // Configurar el fragmento inicial
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void getStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestAllFilesAccessPermission();
            }
        } else {
            requestStoragePermission();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void requestAllFilesAccessPermission() {
        if (!Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    private void requestStoragePermission() {
        if (!checkIfStorageAccessAvailable()) {
            requestStoragePermission.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE});
        }
    }

    private boolean checkIfStorageAccessAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void showSavedLibrariesData() {
        SavedLibraries savedLibraries = SharedPreferenceManager.getInstance(this).getSavedLibrariesData();
        binding.savedLibrariesSection.setVisibility(savedLibraries != null && !(savedLibraries.lists().isEmpty()) ? View.VISIBLE : View.GONE);
        if (savedLibraries != null)
            binding.savedRecyclerView.setAdapter(new SavedLibrariesAdapter(savedLibraries.lists()));
    }

    private void onDrawerItemsClicked() {
        slidingRootNavBuilder.getLayout().findViewById(R.id.settings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            slidingRootNavBuilder.closeMenu();
        });

        slidingRootNavBuilder.getLayout().findViewById(R.id.logo).setOnClickListener(view -> slidingRootNavBuilder.closeMenu());

        slidingRootNavBuilder.getLayout().findViewById(R.id.library).setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, SavedLibrariesActivity.class));
            slidingRootNavBuilder.closeMenu();
        });
    }

    private void updateVersionTextInDrawer() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            View drawerLayout = slidingRootNavBuilder.getLayout();
            if (drawerLayout != null) {
                View versionTextView = drawerLayout.findViewById(R.id.versionTxt);
                if (versionTextView instanceof android.widget.TextView) {
                    ((android.widget.TextView) versionTextView).setText("version " + versionName);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version: " + e.getMessage());
        }
    }

    Handler handler = new Handler();
    Runnable runnable = this::showPlayBarData;

    void showPlayBarData() {
        binding.playBarMusicTitle.setText(ApplicationClass.MUSIC_TITLE);
        binding.playBarMusicDesc.setText(ApplicationClass.MUSIC_DESCRIPTION);
        Picasso.get().load(Uri.parse(ApplicationClass.IMAGE_URL)).into(binding.playBarCoverImage);
        updatePlaybarUi();

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(ApplicationClass.IMAGE_BG_COLOR);
        gradientDrawable.setCornerRadius(18);

        binding.playBarBackground.setBackground(gradientDrawable);

        binding.playBarMusicTitle.setTextColor(ApplicationClass.TEXT_ON_IMAGE_COLOR1);
        binding.playBarMusicDesc.setTextColor(ApplicationClass.TEXT_ON_IMAGE_COLOR1);

        binding.playBarPlayPauseIcon.setImageTintList(ColorStateList.valueOf(ApplicationClass.TEXT_ON_IMAGE_COLOR));
        binding.playBarPrevIcon.setImageTintList(ColorStateList.valueOf(ApplicationClass.TEXT_ON_IMAGE_COLOR));
        binding.playBarNextIcon.setImageTintList(ColorStateList.valueOf(ApplicationClass.TEXT_ON_IMAGE_COLOR));

        OverScrollDecoratorHelper.setUpStaticOverScroll(binding.getRoot(), OverScrollDecoratorHelper.ORIENTATION_VERTICAL);

        handler.postDelayed(runnable, 1000);
    }

    private void updatePlaybarUi() {
        if (ApplicationClass.player != null) {
            binding.playBarPlayPauseIcon.setImageResource(
                    ApplicationClass.player.isPlaying() ?
                            R.drawable.baseline_pause_24 :
                            R.drawable.play_arrow_24px
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NetworkChangeReceiver.registerReceiver(this, networkChangeReceiver);
        showSavedLibrariesData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NetworkChangeReceiver.unregisterReceiver(this, networkChangeReceiver);
    }

    @Override
    public void onBackPressed() {
        if (slidingRootNavBuilder.isMenuOpened())
            slidingRootNavBuilder.closeMenu();
        else
            super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        ApplicationClass.cancelNotification();
        super.onDestroy();
    }

    private void showData() {
        songs.clear();
        artists.clear();
        albums.clear();
        playlists.clear();

        final ApiManager apiManager = new ApiManager(this);

        apiManager.searchSongs(" ", 0, 15, new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                SongSearch songSearch = new Gson().fromJson(response, SongSearch.class);
                Log.i(TAG, "onResponse: " + response);
                if (songSearch.success()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        songSearch.data().results().forEach(results -> {
                            songs.add(new AlbumItem(results.name(), results.language() + " " + results.year(), results.image().get(results.image().size() - 1).url(), results.id()));
                            ActivityMainPopularSongs adapter = new ActivityMainPopularSongs(songs);
                            binding.popularSongsRecyclerView.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        });
                    }
                    ApplicationClass.sharedPreferenceManager.setHomeSongsRecommended(songSearch);
                } else {
                    try {
                        showOfflineData();
                        Toast.makeText(MainActivity.this, new JSONObject(response).getString("message"), Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        Log.e(TAG, "onResponse: ", e);
                    }
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                showOfflineData();
            }
        });

        apiManager.searchArtists(" ", 0, 15, new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                ArtistsSearch artistSearch = new Gson().fromJson(response, ArtistsSearch.class);
                Log.i(TAG, "onResponse: " + response);
                if (artistSearch.success()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        artistSearch.data().results().forEach(results -> {
                            artists.add(results);
                            ActivityMainArtistsItemAdapter adapter = new ActivityMainArtistsItemAdapter(artists);
                            binding.popularArtistsRecyclerView.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        });
                    }
                    ApplicationClass.sharedPreferenceManager.setHomeArtistsRecommended(artistSearch);
                } else {
                    try {
                        showOfflineData();
                        Toast.makeText(MainActivity.this, new JSONObject(response).getString("message"), Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        Log.e(TAG, "onResponse: ", e);
                    }
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                showOfflineData();
            }
        });

        apiManager.searchAlbums(" ", 0, 15, new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                AlbumsSearch albumsSearch = new Gson().fromJson(response, AlbumsSearch.class);
                Log.i(TAG, "onResponse: " + response);
                if (albumsSearch.success()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        albumsSearch.data().results().forEach(results -> {
                            albums.add(new AlbumItem(results.name(), results.language() + " " + results.year(), results.image().get(results.image().size() - 1).url(), results.id()));
                            ActivityMainAlbumItemAdapter adapter = new ActivityMainAlbumItemAdapter(albums);
                            binding.popularAlbumsRecyclerView.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        });
                    }
                    ApplicationClass.sharedPreferenceManager.setHomeAlbumsRecommended(albumsSearch);
                } else {
                    try {
                        Toast.makeText(MainActivity.this, new JSONObject(response).getString("message"), Toast.LENGTH_SHORT).show();
                        showOfflineData();
                    } catch (JSONException e) {
                        Log.e(TAG, "onResponse: ", e);
                    }
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                showOfflineData();
            }
        });

        apiManager.searchPlaylists("2024", 0, 15, new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                PlaylistsSearch playlistsSearch = new Gson().fromJson(response, PlaylistsSearch.class);
                Log.i(TAG, "onResponse: " + response);
                if (playlistsSearch.success()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        playlistsSearch.data().results().forEach(results -> {
                            playlists.add(new AlbumItem(results.name(), "", results.image().get(results.image().size() - 1).url(), results.id()));
                            ActivityMainPlaylistAdapter adapter = new ActivityMainPlaylistAdapter(playlists);
                            binding.playlistRecyclerView.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        });
                    }
                    ApplicationClass.sharedPreferenceManager.setHomePlaylistRecommended(playlistsSearch);
                } else {
                    try {
                        Toast.makeText(MainActivity.this, new JSONObject(response).getString("message"), Toast.LENGTH_SHORT).show();
                        showOfflineData();
                    } catch (JSONException e) {
                        Log.e(TAG, "onResponse: ", e);
                    }
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                showOfflineData();
            }
        });
    }

    private void showShimmerData() {
        final List<AlbumItem> data_shimmer = new ArrayList<>();
        final List<ArtistsSearch.Data.Results> artists_shimmer = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            data_shimmer.add(new AlbumItem("<shimmer>", "<shimmer>", "<shimmer>", "<shimmer>"));
            artists_shimmer.add(new ArtistsSearch.Data.Results(
                    "<shimmer>",
                    "<shimmer>",
                    "<shimmer>",
                    "<shimmer>",
                    "<shimmer>",
                    null
            ));
        }
        binding.popularSongsRecyclerView.setAdapter(new ActivityMainAlbumItemAdapter(data_shimmer));
        binding.popularAlbumsRecyclerView.setAdapter(new ActivityMainAlbumItemAdapter(data_shimmer));
        binding.popularArtistsRecyclerView.setAdapter(new ActivityMainArtistsItemAdapter(artists_shimmer));
        binding.playlistRecyclerView.setAdapter(new ActivityMainPlaylistAdapter(data_shimmer));
    }

    void tryConnect() {
        if (!NetworkUtil.isNetworkAvailable(MainActivity.this)) {
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                Log.e(TAG, "onErrorResponse: ", e);
            }
        }
    }

    private void showOfflineData() {
        if (ApplicationClass.sharedPreferenceManager.getHomeSongsRecommended() != null) {
            SongSearch songSearch = ApplicationClass.sharedPreferenceManager.getHomeSongsRecommended();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                songSearch.data().results().forEach(results -> {
                    songs.add(new AlbumItem(results.name(), results.language() + " " + results.year(), results.image().get(results.image().size() - 1).url(), results.id()));
                    ActivityMainPopularSongs adapter = new ActivityMainPopularSongs(songs);
                    binding.popularSongsRecyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                });
            }
        }

        if (ApplicationClass.sharedPreferenceManager.getHomeArtistsRecommended() != null) {
            ArtistsSearch artistsSearch = ApplicationClass.sharedPreferenceManager.getHomeArtistsRecommended();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                artistsSearch.data().results().forEach(results -> {
                    artists.add(results);
                    ActivityMainArtistsItemAdapter adapter = new ActivityMainArtistsItemAdapter(artists);
                    binding.popularArtistsRecyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                });
            }
        }

        if (ApplicationClass.sharedPreferenceManager.getHomeAlbumsRecommended() != null) {
            AlbumsSearch albumsSearch = ApplicationClass.sharedPreferenceManager.getHomeAlbumsRecommended();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                albumsSearch.data().results().forEach(results -> {
                    albums.add(new AlbumItem(results.name(), results.language() + " " + results.year(), results.image().get(results.image().size() - 1).url(), results.id()));
                    ActivityMainAlbumItemAdapter adapter = new ActivityMainAlbumItemAdapter(albums);
                    binding.popularAlbumsRecyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                });
            }
        }

        if (ApplicationClass.sharedPreferenceManager.getHomePlaylistRecommended() != null) {
            PlaylistsSearch playlistsSearch = ApplicationClass.sharedPreferenceManager.getHomePlaylistRecommended();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                playlistsSearch.data().results().forEach(results -> {
                    playlists.add(new AlbumItem(results.name(), "", results.image().get(results.image().size() - 1).url(), results.id()));
                    ActivityMainPlaylistAdapter adapter = new ActivityMainPlaylistAdapter(playlists);
                    binding.playlistRecyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                });
            }
        }
    }

    private void playBarPopUpAnimation() {
        showPopup();
    }

    private void showPopup() {
        binding.playBarBackground.setVisibility(View.VISIBLE);

        TranslateAnimation slideUp = new TranslateAnimation(0, 0, 1000, 0);
        slideUp.setDuration(500);
        slideUp.setFillAfter(true);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(500);

        slideUp.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {
                binding.playBarBackground.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
            }

            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {
            }
        });

        binding.playBarBackground.startAnimation(slideUp);
    }

    public void closePopup() {
        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(500);
        fadeOut.setFillAfter(true);

        fadeOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {
            }

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                binding.playBarBackground.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {
            }
        });

        binding.playBarBackground.startAnimation(fadeOut);
    }

    public void openSearch(View view) {
        startActivity(new Intent(this, SearchActivity.class));
    }

    private void onClick(View view) {
        if (!ApplicationClass.MUSIC_ID.isBlank()) {
            Intent intent = new Intent(this, MusicOverviewActivity.class);
            intent.putExtra("id", ApplicationClass.MUSIC_ID);
            startActivity(intent);
        }
    }
}
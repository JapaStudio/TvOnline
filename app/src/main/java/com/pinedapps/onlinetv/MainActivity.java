package com.pinedapps.onlinetv;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ArrayList<ModelFirebase> modelFirebaseArrayList;
    private FirebaseAdapter firebaseAdapter;
    private DatabaseReference userFavoritesRef;
    private ValueEventListener userFavoritesListener;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerChannels);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Obtén el usuario actual
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            final String userId = currentUser.getUid(); // Declara userId como final

            // Configurar el listener de autenticación del usuario
            FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser updatedUser = firebaseAuth.getCurrentUser();
                    if (updatedUser != null && !updatedUser.getUid().equals(userId)) {
                        // Usuario ha cambiado, limpiar referencias y configurar para el nuevo usuario
                        cleanupFirebaseListeners();
                        setupFirebaseListeners(updatedUser);
                    }
                }
            };

            FirebaseAuth.getInstance().addAuthStateListener(authStateListener);

            // Configurar los listeners para el usuario actual
            setupFirebaseListeners(currentUser);
        }
    }

    private void setupFirebaseListeners(FirebaseUser currentUser) {
        // Recuperar la lista de canales favoritos del usuario desde la base de datos
        userFavoritesRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child(currentUser.getUid())
                .child("Favorites");

        userFavoritesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Crear un mapa para almacenar los estados de favoritos
                Map<String, Boolean> favoritesMap = new HashMap<>();

                for (DataSnapshot channelSnapshot : snapshot.getChildren()) {
                    String channelId = channelSnapshot.getKey();
                    boolean isFavorite = channelSnapshot.getValue(Boolean.class);

                    // Agregar a mapa de favoritos
                    favoritesMap.put(channelId, isFavorite);
                }

                // Cargar la lista principal de canales desde Firebase
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("ListChannels");
                reference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        modelFirebaseArrayList = new ArrayList<>();

                        for (DataSnapshot readFirebase : snapshot.getChildren()) {
                            ModelFirebase itemsFirebase = readFirebase.getValue(ModelFirebase.class);
                            itemsFirebase.setId(readFirebase.getKey());

                            // Actualizar el estado local de favoritos usando el mapa
                            if (favoritesMap.containsKey(itemsFirebase.getId())) {
                                itemsFirebase.setFavorite(favoritesMap.get(itemsFirebase.getId()));
                            }

                            modelFirebaseArrayList.add(itemsFirebase);
                        }

                        // Configurar el adaptador con la lista actualizada
                        firebaseAdapter = new FirebaseAdapter(modelFirebaseArrayList, MainActivity.this);
                        recyclerView.setAdapter(firebaseAdapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseAdapter", "Error al recuperar la lista de canales: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseAdapter", "Error al recuperar la lista de favoritos del usuario: " + error.getMessage());
            }
        };

        userFavoritesRef.addValueEventListener(userFavoritesListener);
    }

    private void cleanupFirebaseListeners() {
        // Limpiar referencias y listeners anteriores
        if (userFavoritesRef != null && userFavoritesListener != null) {
            userFavoritesRef.removeEventListener(userFavoritesListener);
        }
    }

    private void updateLocalFavoriteState(String channelId, boolean isFavorite) {
        if (modelFirebaseArrayList != null) {
            for (ModelFirebase model : modelFirebaseArrayList) {
                if (model.getId().equals(channelId)) {
                    model.setFavorite(isFavorite);
                    // Notificar al adaptador sobre el cambio
                    int position = modelFirebaseArrayList.indexOf(model);
                    if (position != -1) {
                        firebaseAdapter.notifyItemChanged(position);
                    }
                    return;
                }
            }
        } else {
            Log.e("MainActivity", "modelFirebaseArrayList is null");
        }
    }

    public static class FirebaseAdapter extends RecyclerView.Adapter<FirebaseAdapter.viewHolder> {

        private final ArrayList<ModelFirebase> modelFirebaseArrayList;
        private final Context context;

        public FirebaseAdapter(ArrayList<ModelFirebase> modelFirebaseArrayList, Context context) {
            this.modelFirebaseArrayList = modelFirebaseArrayList;
            this.context = context;
        }

        @NonNull
        @Override
        public FirebaseAdapter.viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false);
            return new viewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FirebaseAdapter.viewHolder holder, int position) {
            holder.bind(modelFirebaseArrayList.get(position));
            Glide.with(context)
                    .load(modelFirebaseArrayList.get(position).getLogo())
                    .into(holder.logo);
        }

        @Override
        public int getItemCount() {
            return modelFirebaseArrayList.size();
        }

        public class viewHolder extends RecyclerView.ViewHolder {

            private final ImageView logo, iconFavorite;
            private final TextView name, gender;

            public viewHolder(@NonNull View itemView) {
                super(itemView);

                logo = itemView.findViewById(R.id.logoChannel);
                name = itemView.findViewById(R.id.nameChanel);
                gender = itemView.findViewById(R.id.genderChannel);
                iconFavorite = itemView.findViewById(R.id.iconFavorite);

                iconFavorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            ModelFirebase currentModel = modelFirebaseArrayList.get(position);

                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (currentUser != null) {
                                String userId = currentUser.getUid();
                                String channelId = currentModel.getId();

                                if (!channelId.isEmpty() && !userId.isEmpty()) {
                                    DatabaseReference userFavoritesRef = FirebaseDatabase.getInstance()
                                            .getReference()
                                            .child("Users")
                                            .child(userId)
                                            .child("Favorites")
                                            .child(channelId);

                                    userFavoritesRef.setValue(!currentModel.isFavorite());
                                } else {
                                    // Log o manejo de error, ya que channelId o userId son nulos o vacíos
                                    Log.e("FirebaseAdapter", "Error: channelId o userId son nulos o vacíos para el modelo en la posición " + position);
                                    // Otra opción: Mostrar un mensaje al usuario
                                    Toast.makeText(context, "Error al marcar como favorito. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
                                }

                                currentModel.setFavorite(!currentModel.isFavorite());
                                notifyItemChanged(position);
                            } else {
                                // El usuario no ha iniciado sesión, manejar esta situación
                                Log.e("FirebaseAdapter", "Error: El usuario no ha iniciado sesión");
                                // Otra opción: Mostrar un mensaje al usuario o redirigir a la pantalla de inicio de sesión
                                Toast.makeText(context, "Inicia sesión para marcar como favorito.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            String videoLink = modelFirebaseArrayList.get(position).getVideo();
                            if (videoLink != null && !videoLink.isEmpty()) {
                                Intent intent = new Intent(context, PlayerActivity.class);
                                intent.putExtra("VIDEO_URL", videoLink);
                                context.startActivity(intent);
                            } else {
                                Toast.makeText(itemView.getContext(), "Enlace de video no disponible", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }

            public void bind(ModelFirebase model) {
                name.setText(model.getName());
                gender.setText(model.getGender());
                Glide.with(context).load(model.getLogo()).into(logo);

                if (model.isFavorite()) {
                    iconFavorite.setImageResource(R.drawable.ic_favorite_filled);
                } else {
                    iconFavorite.setImageResource(R.drawable.ic_favorite_outline);
                }
            }
        }
    }
}
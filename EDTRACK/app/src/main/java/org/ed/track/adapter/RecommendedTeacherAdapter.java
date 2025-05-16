package org.ed.track.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.ed.track.chat.ChatActivity;
import org.ed.track.R;
import org.ed.track.model.UserProfile;
import org.ed.track.student.TeacherCoursesActivity;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class RecommendedTeacherAdapter extends RecyclerView.Adapter<RecommendedTeacherAdapter.TeacherViewHolder> {

    private Context context;
    private List<UserProfile> teacherList;

    public RecommendedTeacherAdapter(Context context, List<UserProfile> teacherList) {
        this.context = context;
        this.teacherList = teacherList;
    }

    public void updateList(List<UserProfile> newList) {
        teacherList.clear();
        teacherList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recommended_teacher, parent, false);
        return new TeacherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        UserProfile teacher = teacherList.get(position);
        holder.nameText.setText(teacher.getName());
        holder.areaText.setText(teacher.getLocation());
        holder.qualificationText.setText(teacher.getQualification());

        Log.e("name", "onBindViewHolder: " + teacher.getName() + " id: " + teacher.getUserId());
        // Load profile image (if available)
        if (teacher.getImageUrl() != null && !teacher.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(teacher.getImageUrl())
                    .placeholder(R.drawable.baseline_person_24)
                    .into(holder.profileImage);
        }
        holder.messageTeacher.setOnClickListener(v -> {
            // Handle message teacher button click
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("receiverId", teacher.getUserId());
            context.startActivity(intent);
        });

        holder.teacherClick.setOnClickListener(view -> {
            // Handle teacher click
            Intent intent = new Intent(context, TeacherCoursesActivity.class);
            intent.putExtra("teacher_id", teacher.getUserId());
            intent.putExtra("teacher_name", teacher.getName());
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return teacherList.size();
    }

    public static class TeacherViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, areaText, qualificationText;
        CircleImageView profileImage;
        ImageView messageTeacher;
        CardView teacherClick;

        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.teacher_name);
            areaText = itemView.findViewById(R.id.teacher_area);
            qualificationText = itemView.findViewById(R.id.teacher_qualification);
            profileImage = itemView.findViewById(R.id.teacher_image);
            messageTeacher = itemView.findViewById(R.id.messageTeacher);
            teacherClick = itemView.findViewById(R.id.teacherClick);
        }
    }
}



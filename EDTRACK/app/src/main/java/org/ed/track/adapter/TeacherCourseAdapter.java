package org.ed.track.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.ed.track.model.CourseModel;
import org.ed.track.R;

import java.util.List;

public class TeacherCourseAdapter extends RecyclerView.Adapter<TeacherCourseAdapter.CourseViewHolder> {

    private List<CourseModel> courseList;

    public TeacherCourseAdapter(List<CourseModel> courseList) {
        this.courseList = courseList;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        CourseModel course = courseList.get(position);
        holder.tvCourseName.setText(course.getTitle());
        holder.tvCourseDesc.setText(course.getDescription());
        holder.tvCoursePrice.setText("Price: " + course.getPrice() + " PKR");

        Glide.with(holder.itemView.getContext()).load(course.getImageUrl()).error(R.drawable.baseline_person_24).into(holder.courseImage);
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseName, tvCourseDesc, tvCoursePrice;
        ImageView courseImage;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvCourseDesc = itemView.findViewById(R.id.tvCourseDesc);
            tvCoursePrice = itemView.findViewById(R.id.tvCoursePrice);
            courseImage = itemView.findViewById(R.id.courseImage);
        }
    }
}


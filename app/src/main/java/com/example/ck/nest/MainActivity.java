package com.example.ck.nest;

import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private String TAG=getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        RecyclerView recyclerView= (RecyclerView) findViewById(R.id.recyclerview);
        MyListenerBehavior behavior = MyListenerBehavior.getBehavior(recyclerView);
        behavior.setCanOverScroll(500)
                .setScrollListener(new MyListenerBehavior.OnScrollListener() {
                    @Override
                    public void onScroll(int scroll) {
                        Log.i(TAG, "onScroll: "+scroll);

                    }

                    @Override
                    public void overScroll(int overScroll) {
                        Log.i(TAG, "overScroll: "+overScroll);
                    }
                });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                TextView textView = new TextView(parent.getContext());
                textView.setText("dededsxxxxxxxxxxxxx");
                textView.setHeight(200);
                return new RecyclerView.ViewHolder(textView) {
                    @Override
                    public String toString() {
                        return super.toString();
                    }
                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            }

            @Override
            public int getItemCount() {
                return 20;
            }
        });
    }
}

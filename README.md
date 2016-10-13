# Cyclic Android View Pager implementation

###Features
- Cyclic scroll views
- Can use fragments from support repository
- Can implement custom adapter
- Do not duplicate first and last views, used bitmap variants instead
- Cyclic works with 1, 2 or more views
- Scrolling stops if next element is null
- View caching, remove old views for free memory

### Simple example
After create CyclicView your should set adapter

```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CyclicView cyclicView = (CyclicView) findViewById(R.id.cyclic_view);
        cyclicView.setAdapter(new CyclicAdapter() {
            @Override
            public int getItemsCount() {
                return 10;
            }

            @Override
            public View createView(int position) {
                TextView textView = new TextView(MainActivity.this);
                textView.setText(String.format("TextView #%d", position + 1));
                return textView;
            }

            @Override
            public void removeView(int position, View view) {
                // Do nothing
            }
        });
    }
}
```
###Simple example with support fragments

```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CyclicView cyclicView = (CyclicView) findViewById(R.id.cyclic_view);
        cyclicView.setAdapter(new CyclicFragmentAdapter(this, getSupportFragmentManager()) {
            @Override
            public int getItemsCount() {
                return 10;
            }

            @Override
            protected Fragment createFragment(int position) {
                String text = String.format("TextView #%d", position + 1);
                return TextFragment.newInstance(text);
            }
        });
    }

    public static class TextFragment extends Fragment {
        private static final String TEXT_ARG = "TEXT_ARG";

        public static Fragment newInstance(String text) {
            Bundle args = new Bundle();
            args.putString(TEXT_ARG, text);
            Fragment fragment = new TextFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            String text = getArguments().getString(TEXT_ARG);
            TextView textView = new TextView(getContext());
            textView.setText(text);
            return textView;
        }
    }
}
```
###Available methods: 
`setAdapter(CyclicAdapter)` for setup a adapter 
`setCurrentPosition(int)` for switch current position 
`refreshViewsAroundCurrent()` for reload null views around current position 
`addOnPositionChangeListener(CyclicView.OnPositionChangeListener)` for observe CyclicView on position change 

### How add to project
Add it to your build.gradle with:

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
and:
```gradle
dependencies {
    compile 'com.github.pozitiffcat:cyclicview:1.0.1'
}
```




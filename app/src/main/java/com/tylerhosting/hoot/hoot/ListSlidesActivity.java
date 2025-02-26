package com.tylerhosting.hoot.hoot;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;

public class ListSlidesActivity extends SlidesActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        word.addTextChangedListener(wordWatcher);
//        definition.setTypeface(listfont);
        lv.setVisibility(View.GONE);
        tvalpha = findViewById(R.id.tvAlphagram);
        tvalpha.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    protected void loadBundle() {
        Intent intentExtras = getIntent();

        Bundle extrasBundle = ((Intent) intentExtras).getExtras();
        if (extrasBundle.isEmpty())
            return;

//        String term = extrasBundle.getString("term");
//        if (term == null)
//            term = "";

//        String search = extrasBundle.getString("search");
        String desc = extrasBundle.getString("desc");

        header.setText("Slides for " + desc);
        //header.setText("Slides for " + search + " " + term);

//        String abcd[] = extrasBundle.getStringArray("Words");
        String[] inbound = extrasBundle.getStringArray("Words");
        wordlist = new SpannableString[inbound.length];
        if (inbound.length == 0)
            return;

        if (LexData.getColorBlanks()) {

            // for each word

            for (int wordId = 0; wordId < inbound.length; wordId++) {
                boolean hasBlanks = false;

                SpannableStringBuilder sb = new SpannableStringBuilder();
                // for each letter
                for (int index = 0; index < inbound[wordId].length(); index++) {
                    char letter = inbound[wordId].charAt(index);
                    if (Character.isLowerCase(letter)) {
                        hasBlanks = true;

                        switch (themeName) {
                            case "Dark Theme":
                                sb.append("<font color='#00ff88'>" + (char) (letter - 32) + "</font>"); // #00ff88
                                break;
                            case "Light Theme":
                            default:
                                sb.append("<font color='#0033ee'>" + (char) (letter - 32) + "</font>"); // #0033ee
                                break;
                        }

                    } else
                        sb.append(inbound[wordId].charAt(index));
                }
//                Log.i("Word", wordCol + ": " + sb);

                if (hasBlanks) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wordlist[wordId] = SpannableString.valueOf((Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)));
                    } else
                        wordlist[wordId] = SpannableString.valueOf((Html.fromHtml(sb.toString())));
                }
                else
                    wordlist[wordId] = SpannableString.valueOf(inbound[wordId]);
//                else
//                    wordview.setText(wordCol);
            }
        }
        else {
            for (int wordId = 0; wordId < inbound.length; wordId++)
                wordlist[wordId] = SpannableString.valueOf(inbound[wordId]);
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.spincenter, inbound);
        selector.setAdapter(dataAdapter);
        selector.setOnItemSelectedListener(selection);

    }

    public void showSlides() {
        word.setText(wordlist[0]);
    }

    private final TextWatcher wordWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            databaseAccess.open();
            definition.setText(databaseAccess.getDefinition(String.valueOf(wordlist[currentID])));
            int position = currentID + 1;
            status.setText("Showing " + position + "/" + wordlist.length + " words from " + LexData.getLexName());

            selector.setSelection(currentID);

            databaseAccess.close();
            answer.setText(databaseAccess.sortString(wordlist[currentID].toString()));

        }
    };


}

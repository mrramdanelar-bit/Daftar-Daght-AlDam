
package dz.daftaridaght;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.InputType;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    DB db;
    LinearLayout root;
    int activeUserId = -1;
    String activeUserName = "";

    final int GREEN = Color.rgb(23,107,99);
    final int DARK_GREEN = Color.rgb(20,86,79);
    final int BG = Color.rgb(243,247,246);
    final int TEXT = Color.rgb(23,43,42);
    final int MUTED = Color.rgb(100,119,115);
    final int BORDER = Color.rgb(225,234,232);

    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(Color.rgb(20,70,65));
        db = new DB(this);
        showUsers();
    }

    int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + .5f);
    }

    GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    GradientDrawable cardShape() {
        GradientDrawable d = shape(Color.WHITE,18);
        d.setStroke(dp(1),BORDER);
        return d;
    }

    TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(TEXT);
        if (bold) t.setTypeface(null,Typeface.BOLD);
        t.setPadding(0,dp(5),0,dp(5));
        return t;
    }

    TextView muted(String value) {
        TextView t = text(value,13,false);
        t.setTextColor(MUTED);
        return t;
    }

    LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return l;
    }

    LinearLayout card() {
        LinearLayout l = column();
        l.setPadding(dp(16),dp(14),dp(16),dp(14));
        l.setBackground(cardShape());
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,dp(7),0,dp(7));
        root.addView(l,p);
        return l;
    }

    void space(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v,new LinearLayout.LayoutParams(1,dp(height)));
    }

    Button button(String value, boolean primary) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(14);
        b.setTypeface(null,Typeface.BOLD);
        b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : DARK_GREEN);
        b.setPadding(dp(12),dp(5),dp(12),dp(5));
        b.setBackground(shape(
            primary ? GREEN : Color.rgb(232,241,239),12));
        return b;
    }

    EditText edit(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setInputType(type);
        e.setPadding(dp(12),dp(10),dp(12),dp(10));
        e.setBackground(shape(Color.WHITE,11));
        e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return e;
    }

    void base(String title) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        root = column();
        root.setPadding(dp(12),dp(12),dp(12),dp(24));
        root.setBackgroundColor(BG);

        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = column();
        header.setPadding(dp(20),dp(18),dp(20),dp(18));
        header.setBackground(shape(GREEN,20));
        root.addView(header);

        TextView h = text("دفتر ضغط الدم",25,true);
        h.setTextColor(Color.WHITE);
        header.addView(h);

        TextView sub = text(title,15,false);
        sub.setTextColor(Color.rgb(225,243,239));
        header.addView(sub);

        space(root,10);
    }

    void showUsers() {
        activeUserId = -1;
        base("متابعة قياساتك المنزلية");

        LinearLayout c = card();
        c.addView(text("مرحبًا بك 👋",20,true));
        c.addView(muted(
            "اختر الشخص لفتح سجله الخاص، أو أضف مستخدمًا جديدًا."
        ));

        Button add = button("＋ إضافة مستخدم جديد",true);
        c.addView(add);
        add.setOnClickListener(v -> addUserDialog());

        root.addView(text("المستخدمون",19,true));

        Cursor cur = db.getReadableDatabase().rawQuery(
            "SELECT id,name FROM users ORDER BY name COLLATE NOCASE",
            null
        );

        if (!cur.moveToFirst()) {
            LinearLayout empty = card();
            TextView t = text("لا يوجد مستخدمون بعد.",16,true);
            t.setGravity(Gravity.CENTER);
            empty.addView(t);
            empty.addView(muted("ابدأ بإضافة أول مستخدم."));
        } else {
            do {
                final int id = cur.getInt(0);
                final String name = cur.getString(1);

                LinearLayout item = card();
                item.addView(text("👤  " + name,19,true));

                Button open = button("فتح السجل",true);
                item.addView(open);
                open.setOnClickListener(v -> {
                    activeUserId = id;
                    activeUserName = name;
                    showProfile();
                });

                Button del = button("حذف المستخدم وسجله",false);
                item.addView(del);
                del.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                        .setTitle("حذف المستخدم")
                        .setMessage(
                            "سيتم حذف جميع قياسات " + name +
                            ". هل تريد المتابعة؟"
                        )
                        .setNegativeButton("إلغاء",null)
                        .setPositiveButton("حذف",(d,w)->{
                            db.deleteUser(id);
                            showUsers();
                        }).show()
                );
            } while (cur.moveToNext());
        }
        cur.close();
    }

    void addUserDialog() {
        EditText name = edit("اكتب اسم المستخدم",InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
            .setTitle("إضافة مستخدم جديد")
            .setView(name)
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("حفظ",(d,w)->{
                String n = name.getText().toString().trim();
                if (n.isEmpty()) {
                    toast("يرجى كتابة الاسم");
                    return;
                }
                db.addUser(n);
                showUsers();
            }).show();
    }

    void showProfile() {
        base("سجل: " + activeUserName);

        Button back = button("← العودة إلى المستخدمين",false);
        root.addView(back);
        back.setOnClickListener(v -> showUsers());

        LinearLayout form = card();

        form.addView(text("تسجيل قياس جديد",20,true));
        form.addView(muted("أدخل قراءة الضغط المسجلة من جهازك."));

        EditText sys1 = edit("الانقباضي SYS - مثال 120",
            InputType.TYPE_CLASS_NUMBER);
        EditText dia1 = edit("الانبساطي DIA - مثال 80",
            InputType.TYPE_CLASS_NUMBER);

        EditText sys2 = edit("الانقباضي - قراءة ثانية اختيارية",
            InputType.TYPE_CLASS_NUMBER);
        EditText dia2 = edit("الانبساطي - قراءة ثانية اختيارية",
            InputType.TYPE_CLASS_NUMBER);

        EditText pulse = edit("النبض (اختياري)",
            InputType.TYPE_CLASS_NUMBER);
        EditText note = edit("ملاحظات (اختياري)",
            InputType.TYPE_CLASS_TEXT);

        form.addView(text("القراءة الأولى",16,true));
        form.addView(sys1);
        space(form,7);
        form.addView(dia1);

        space(form,10);
        form.addView(text("القراءة الثانية (اختيارية)",16,true));
        form.addView(sys2);
        space(form,7);
        form.addView(dia2);

        space(form,10);
        form.addView(pulse);
        space(form,7);
        form.addView(note);

        Spinner period = new Spinner(this);
        String[] periods = {
            "صباحًا","مساءً","وقت آخر"
        };
        period.setAdapter(new ArrayAdapter<String>(
            this,android.R.layout.simple_spinner_dropdown_item,periods
        ));

        space(form,8);
        form.addView(text("فترة القياس",14,true));
        form.addView(period);

        TextView average = text("",17,true);
        average.setTextColor(GREEN);
        form.addView(average);

        TextWatcherHelper watcher = new TextWatcherHelper(
            sys1,dia1,sys2,dia2,average
        );
        watcher.attach();

        Button save = button("حفظ القياس",true);
        form.addView(save);

        save.setOnClickListener(v -> {
            try {
                String aText = sys1.getText().toString().trim();
                String bText = dia1.getText().toString().trim();
                String cText = sys2.getText().toString().trim();
                String dText = dia2.getText().toString().trim();

                if (aText.isEmpty() || bText.isEmpty()) {
                    toast("أدخل القراءة الأولى");
                    return;
                }

                int a = Integer.parseInt(aText);
                int b = Integer.parseInt(bText);

                Integer c = null;
                Integer d = null;

                if (!cText.isEmpty() || !dText.isEmpty()) {
                    if (cText.isEmpty() || dText.isEmpty()) {
                        toast("أكمل القراءة الثانية أو اتركها فارغة");
                        return;
                    }
                    c = Integer.parseInt(cText);
                    d = Integer.parseInt(dText);
                }

                if (!valid(a,b) || (c != null && !valid(c,d))) {
                    toast("تحقق من قيم الضغط المدخلة");
                    return;
                }

                Integer p = null;
                String pulseText = pulse.getText().toString().trim();

                if (!pulseText.isEmpty()) {
                    p = Integer.parseInt(pulseText);
                    if (p < 20 || p > 250) {
                        toast("تحقق من قيمة النبض");
                        return;
                    }
                }

                db.addMeasure(
                    activeUserId,a,b,c,d,p,
                    period.getSelectedItem().toString(),
                    note.getText().toString().trim()
                );

                toast("تم حفظ القياس بنجاح");
                showProfile();

            } catch(Exception ex) {
                toast("يرجى التأكد من إدخال الأرقام بصورة صحيحة");
            }
        });

        root.addView(text("سجل القياسات",20,true));

        Cursor cur = db.getReadableDatabase().rawQuery(
            "SELECT id,created,sys1,dia1,sys2,dia2,pulse,period,note " +
            "FROM readings WHERE user_id=? ORDER BY id DESC",
            new String[]{String.valueOf(activeUserId)}
        );

        if (!cur.moveToFirst()) {
            LinearLayout empty = card();
            TextView t = text("لا توجد قياسات مسجلة بعد.",15,true);
            t.setGravity(Gravity.CENTER);
            empty.addView(t);
        } else {
            do {
                final int id = cur.getInt(0);
                String date = cur.getString(1);
                int s1 = cur.getInt(2);
                int d1 = cur.getInt(3);

                Integer s2 = cur.isNull(4) ? null : cur.getInt(4);
                Integer d2 = cur.isNull(5) ? null : cur.getInt(5);

                String p = cur.isNull(6) ?
                    "غير مسجل" : String.valueOf(cur.getInt(6));

                String per = cur.getString(7);
                String nt = cur.getString(8);

                LinearLayout item = card();

                item.addView(text(date + "  •  " + per,14,true));

                String reading = s1 + "/" + d1;
                if (s2 != null) reading += "  |  " + s2 + "/" + d2;

                TextView values = text(reading + " mmHg",23,true);
                values.setTextColor(GREEN);
                item.addView(values);

                if (s2 != null) {
                    item.addView(muted(
                        "متوسط القراءتين: " +
                        Math.round((s1+s2)/2.0) + " / " +
                        Math.round((d1+d2)/2.0)
                    ));
                }

                item.addView(muted("النبض: " + p));

                if (nt != null && !nt.isEmpty())
                    item.addView(muted("ملاحظة: " + nt));

                Button edit = button("✏️ تعديل القياس",false);
                item.addView(edit);
                edit.setOnClickListener(v ->
                    editMeasureDialog(id,s1,d1,s2,d2,p,per,nt)
                );

                Button delete = button("حذف القياس",false);
                item.addView(delete);
                delete.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                        .setMessage("هل تريد حذف هذا القياس؟")
                        .setNegativeButton("إلغاء",null)
                        .setPositiveButton("حذف",(dd,w)->{
                            db.deleteMeasure(id);
                            showProfile();
                        }).show()
                );

            } while(cur.moveToNext());
        }
        cur.close();

        LinearLayout reminder = card();
        reminder.addView(text("تذكير للقياس الصحيح",17,true));
        reminder.addView(muted(
            "• استرح بهدوء 5 دقائق قبل القياس.\n" +
            "• اجلس وظهرك مسنود وقدماك على الأرض.\n" +
            "• ضع السوار على الذراع بمستوى القلب.\n" +
            "• تجنب القهوة والتدخين والرياضة خلال 30 دقيقة قبل القياس."
        ));
    }

    boolean valid(int s,int d) {
        return s >= 50 && s <= 280 &&
               d >= 30 && d <= 180 && s > d;
    }

    void editMeasureDialog(int id,int s1,int d1,
                           Integer s2,Integer d2,String pulseValue,
                           String per,String noteValue) {

        LinearLayout layout = column();
        layout.setPadding(dp(15),dp(8),dp(15),dp(8));

        EditText e1 = edit("الانقباضي SYS",InputType.TYPE_CLASS_NUMBER);
        EditText e2 = edit("الانبساطي DIA",InputType.TYPE_CLASS_NUMBER);
        EditText e3 = edit("الانقباضي - القراءة الثانية اختيارية",
            InputType.TYPE_CLASS_NUMBER);
        EditText e4 = edit("الانبساطي - القراءة الثانية اختيارية",
            InputType.TYPE_CLASS_NUMBER);
        EditText ep = edit("النبض (اختياري)",InputType.TYPE_CLASS_NUMBER);
        EditText en = edit("ملاحظات",InputType.TYPE_CLASS_TEXT);

        e1.setText(String.valueOf(s1));
        e2.setText(String.valueOf(d1));

        if (s2 != null) e3.setText(String.valueOf(s2));
        if (d2 != null) e4.setText(String.valueOf(d2));
        if (!pulseValue.equals("غير مسجل")) ep.setText(pulseValue);
        en.setText(noteValue);

        layout.addView(text("القراءة الأولى",15,true));
        layout.addView(e1);
        layout.addView(e2);
        space(layout,8);
        layout.addView(text("القراءة الثانية الاختيارية",15,true));
        layout.addView(e3);
        layout.addView(e4);
        space(layout,8);
        layout.addView(ep);
        layout.addView(en);

        Spinner sp = new Spinner(this);
        String[] periods = {"صباحًا","مساءً","وقت آخر"};
        sp.setAdapter(new ArrayAdapter<String>(
            this,android.R.layout.simple_spinner_dropdown_item,periods
        ));

        for (int i=0;i<periods.length;i++) {
            if (periods[i].equals(per)) sp.setSelection(i);
        }
        layout.addView(sp);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);

        new AlertDialog.Builder(this)
            .setTitle("تعديل القياس")
            .setView(scroll)
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("حفظ التعديلات",(dialog,which)->{
                try {
                    int a = Integer.parseInt(e1.getText().toString().trim());
                    int b = Integer.parseInt(e2.getText().toString().trim());

                    String cText = e3.getText().toString().trim();
                    String dText = e4.getText().toString().trim();

                    Integer c = null;
                    Integer d = null;

                    if (!cText.isEmpty() || !dText.isEmpty()) {
                        if (cText.isEmpty() || dText.isEmpty()) {
                            toast("أكمل القراءة الثانية أو اتركها فارغة");
                            return;
                        }
                        c = Integer.parseInt(cText);
                        d = Integer.parseInt(dText);
                    }

                    if (!valid(a,b) || (c != null && !valid(c,d))) {
                        toast("تحقق من قيم الضغط");
                        return;
                    }

                    Integer p = null;
                    String pv = ep.getText().toString().trim();

                    if (!pv.isEmpty()) {
                        p = Integer.parseInt(pv);
                        if (p < 20 || p > 250) {
                            toast("تحقق من قيمة النبض");
                            return;
                        }
                    }

                    db.updateMeasure(
                        id,a,b,c,d,p,
                        sp.getSelectedItem().toString(),
                        en.getText().toString().trim()
                    );

                    toast("تم تعديل القياس");
                    showProfile();

                } catch(Exception ex) {
                    toast("يرجى التأكد من صحة الأرقام");
                }
            }).show();
    }

    void toast(String s) {
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }

    class TextWatcherHelper {
        EditText a,b,c,d;
        TextView result;

        TextWatcherHelper(EditText a,EditText b,EditText c,
                          EditText d,TextView result) {
            this.a=a;
            this.b=b;
            this.c=c;
            this.d=d;
            this.result=result;
        }

        void attach() {
            android.text.TextWatcher w =
                new android.text.TextWatcher() {
                    public void beforeTextChanged(
                        CharSequence s,int start,int count,int after) {}
                    public void onTextChanged(
                        CharSequence s,int start,int before,int count) {
                        calculate();
                    }
                    public void afterTextChanged(
                        android.text.Editable e) {}
                };

            a.addTextChangedListener(w);
            b.addTextChangedListener(w);
            c.addTextChangedListener(w);
            d.addTextChangedListener(w);
        }

        void calculate() {
            try {
                String av=a.getText().toString().trim();
                String bv=b.getText().toString().trim();
                String cv=c.getText().toString().trim();
                String dv=d.getText().toString().trim();

                if (av.isEmpty() || bv.isEmpty() ||
                    cv.isEmpty() || dv.isEmpty()) {
                    result.setText("");
                    return;
                }

                int x=Integer.parseInt(av);
                int y=Integer.parseInt(bv);
                int z=Integer.parseInt(cv);
                int q=Integer.parseInt(dv);

                if (!valid(x,y) || !valid(z,q)) {
                    result.setText("");
                    return;
                }

                result.setText(
                    "متوسط القراءتين: " +
                    Math.round((x+z)/2.0) + " / " +
                    Math.round((y+q)/2.0) + " mmHg"
                );

            } catch(Exception ex) {
                result.setText("");
            }
        }
    }

    class DB extends SQLiteOpenHelper {

        DB(Context c) {
            super(c,"pressure_diary_v2.db",null,1);
        }

        public void onCreate(SQLiteDatabase d) {
            d.execSQL(
                "CREATE TABLE users(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL)"
            );

            d.execSQL(
                "CREATE TABLE readings(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "created TEXT NOT NULL," +
                "sys1 INTEGER NOT NULL," +
                "dia1 INTEGER NOT NULL," +
                "sys2 INTEGER," +
                "dia2 INTEGER," +
                "pulse INTEGER," +
                "period TEXT," +
                "note TEXT," +
                "FOREIGN KEY(user_id) REFERENCES users(id))"
            );
        }

        public void onUpgrade(SQLiteDatabase d,int oldV,int newV) {}

        void addUser(String name) {
            ContentValues v = new ContentValues();
            v.put("name",name);
            getWritableDatabase().insert("users",null,v);
        }

        void deleteUser(int id) {
            SQLiteDatabase d = getWritableDatabase();
            d.delete("readings","user_id=?",
                new String[]{String.valueOf(id)});
            d.delete("users","id=?",
                new String[]{String.valueOf(id)});
        }

        void deleteMeasure(int id) {
            getWritableDatabase().delete("readings",
                "id=?",new String[]{String.valueOf(id)});
        }

        void addMeasure(int uid,int s1,int d1,Integer s2,
                        Integer d2,Integer pulse,String period,
                        String note) {

            ContentValues v = new ContentValues();
            v.put("user_id",uid);
            v.put("created",new SimpleDateFormat(
                "yyyy-MM-dd HH:mm",Locale.getDefault()
            ).format(new Date()));

            v.put("sys1",s1);
            v.put("dia1",d1);

            if (s2 != null) v.put("sys2",s2);
            if (d2 != null) v.put("dia2",d2);
            if (pulse != null) v.put("pulse",pulse);

            v.put("period",period);
            v.put("note",note);

            getWritableDatabase().insert("readings",null,v);
        }

        void updateMeasure(int id,int s1,int d1,Integer s2,
                           Integer d2,Integer pulse,String period,
                           String note) {

            ContentValues v = new ContentValues();
            v.put("sys1",s1);
            v.put("dia1",d1);

            if (s2 == null) v.putNull("sys2");
            else v.put("sys2",s2);

            if (d2 == null) v.putNull("dia2");
            else v.put("dia2",d2);

            if (pulse == null) v.putNull("pulse");
            else v.put("pulse",pulse);

            v.put("period",period);
            v.put("note",note);

            getWritableDatabase().update("readings",v,
                "id=?",new String[]{String.valueOf(id)});
        }
    }
}

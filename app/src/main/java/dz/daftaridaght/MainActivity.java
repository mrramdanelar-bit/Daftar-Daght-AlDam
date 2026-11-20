package dz.daftaridaght;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    DB db;
    LinearLayout root, body;
    int activeUserId = -1;
    String activeUserName = "";
    int pad = 18;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new DB(this);
        showUsers();
    }

    int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    TextView text(String s, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size);
        t.setTextColor(0xff183044);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(0, dp(6), 0, dp(6));
        return t;
    }

    Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        return b;
    }

    EditText edit(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(type);
        e.setSingleLine(true);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        return e;
    }

    void base(String title) {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(pad), dp(18), dp(pad), dp(24));
        root.setBackgroundColor(0xfff4f8fb);
        scroll.addView(root);
        setContentView(scroll);
        TextView h = text(title, 24, true);
        h.setTextColor(0xff126782);
        root.addView(h);
        root.addView(text("دفتر ضغط الدم", 13, false));
        View line = new View(this);
        line.setBackgroundColor(0xffd8e4eb);
        root.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    void showUsers() {
        activeUserId = -1;
        base("المستخدمون");
        root.addView(text("اختر اسم الشخص لفتح سجله الخاص، أو أضف مستخدمًا جديدًا.", 16, false));
        root.addView(button("＋ إضافة مستخدم جديد"));
        Button add = (Button)root.getChildAt(root.getChildCount()-1);
        add.setOnClickListener(v -> addUserDialog());

        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,name FROM users ORDER BY name COLLATE NOCASE", null);
        if (c.getCount() == 0) root.addView(text("لا يوجد مستخدمون بعد. ابدأ بإضافة اسم.", 16, false));
        while (c.moveToNext()) {
            final int id = c.getInt(0);
            final String name = c.getString(1);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(10), dp(14), dp(10));
            card.setBackgroundColor(0xffffffff);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, dp(8), 0, dp(4));
            root.addView(card, lp);
            TextView n = text("👤  " + name, 19, true);
            card.addView(n);
            Button open = button("فتح صفحة " + name);
            card.addView(open);
            open.setOnClickListener(v -> { activeUserId=id; activeUserName=name; showProfile(); });
            Button del = button("حذف المستخدم وسجله");
            card.addView(del);
            del.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("حذف المستخدم")
                .setMessage("سيتم حذف جميع قياسات " + name + ". هل تريد المتابعة؟")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حذف", (d,w) -> { db.deleteUser(id); showUsers(); }).show());
        }
        c.close();
    }

    void addUserDialog() {
        EditText name = edit("اكتب اسم المستخدم", 1);
        new AlertDialog.Builder(this).setTitle("مستخدم جديد").setView(name)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ", (d,w) -> {
                String n = name.getText().toString().trim();
                if (n.isEmpty()) { toast("يرجى كتابة الاسم"); return; }
                db.addUser(n); showUsers();
            }).show();
    }

    void showProfile() {
        base("سجل: " + activeUserName);
        Button back = button("← العودة إلى المستخدمين");
        root.addView(back);
        back.setOnClickListener(v -> showUsers());

        root.addView(text("تسجيل قياس جديد", 19, true));
        root.addView(text("أدخل قراءتين يفصل بينهما نحو دقيقة واحدة.", 14, false));
        EditText sys1 = edit("الانقباضي SYS - القراءة الأولى", 2);
        EditText dia1 = edit("الانبساطي DIA - القراءة الأولى", 2);
        EditText sys2 = edit("الانقباضي SYS - القراءة الثانية", 2);
        EditText dia2 = edit("الانبساطي DIA - القراءة الثانية", 2);
        EditText pulse = edit("النبض (اختياري)", 2);
        EditText note = edit("ملاحظات (اختياري)", 1);
        root.addView(sys1); root.addView(dia1); root.addView(sys2);
        root.addView(dia2); root.addView(pulse); root.addView(note);
        Spinner period = new Spinner(this);
        String[] periods = {"الفترة: صباحًا", "الفترة: مساءً", "الفترة: وقت آخر"};
        period.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, periods));
        root.addView(period);
        Button save = button("حفظ القياس");
        root.addView(save);
        TextView average = text("", 16, true);
        root.addView(average);

        Runnable calc = () -> {
            try {
                double a=Double.parseDouble(sys1.getText().toString());
                double b=Double.parseDouble(dia1.getText().toString());
                double c=Double.parseDouble(sys2.getText().toString());
                double d=Double.parseDouble(dia2.getText().toString());
                average.setText(String.format(Locale.US, "المتوسط: %.0f / %.0f mmHg", (a+c)/2, (b+d)/2));
            } catch(Exception ex) { average.setText(""); }
        };
        save.setOnClickListener(v -> {
            try {
                int a=Integer.parseInt(sys1.getText().toString().trim());
                int b=Integer.parseInt(dia1.getText().toString().trim());
                int c=Integer.parseInt(sys2.getText().toString().trim());
                int d=Integer.parseInt(dia2.getText().toString().trim());
                if(a<50||a>280||c<50||c>280||b<30||b>180||d<30||d>180) {
                    toast("تحقق من قيم الضغط المدخلة"); return;
                }
                Integer p=null;
                if(!pulse.getText().toString().trim().isEmpty()) p=Integer.parseInt(pulse.getText().toString().trim());
                db.addMeasure(activeUserId, a,b,c,d,p,period.getSelectedItem().toString(),note.getText().toString().trim());
                toast("تم حفظ القياس");
                showProfile();
            } catch(Exception ex) { toast("أدخل قراءتي الضغط بشكل صحيح"); }
        });

        root.addView(text("القياسات السابقة", 20, true));
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,created,sys1,dia1,sys2,dia2,pulse,period,note FROM measures WHERE user_id=? ORDER BY id DESC",
            new String[]{String.valueOf(activeUserId)});
        if(c.getCount()==0) root.addView(text("لا توجد قياسات مسجلة لهذا المستخدم.", 15, false));
        while(c.moveToNext()) {
            int id=c.getInt(0);
            String date=c.getString(1);
            int s1=c.getInt(2), d1=c.getInt(3), s2=c.getInt(4), d2=c.getInt(5);
            String p=c.isNull(6) ? "غير مسجل" : String.valueOf(c.getInt(6));
            String per=c.getString(7), nt=c.getString(8);
            LinearLayout item=new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(12),dp(8),dp(12),dp(8));
            item.setBackgroundColor(0xffffffff);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.setMargins(0,dp(7),0,0); root.addView(item,lp);
            item.addView(text(date+"  •  "+per,13,true));
            item.addView(text("الأولى: "+s1+"/"+d1+"   |   الثانية: "+s2+"/"+d2,15,false));
            item.addView(text(String.format(Locale.US,"المتوسط: %.0f / %.0f mmHg", (s1+s2)/2.0, (d1+d2)/2.0),17,true));
            item.addView(text("النبض: "+p+(nt.isEmpty()?"":"   •   "+nt),13,false));
            Button delete=button("حذف هذا القياس"); item.addView(delete);
            delete.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("حذف هذا القياس؟")
                .setNegativeButton("إلغاء",null).setPositiveButton("حذف",(dd,w)->{db.deleteMeasure(id);showProfile();}).show());
        }
        c.close();
    }

    void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }

    class DB extends SQLiteOpenHelper {
        DB(Context c) { super(c,"pressure_diary.db",null,1); }
        public void onCreate(SQLiteDatabase d) {
            d.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");
            d.execSQL("CREATE TABLE measures(id INTEGER PRIMARY KEY AUTOINCREMENT,user_id INTEGER NOT NULL,created TEXT,sys1 INTEGER,dia1 INTEGER,sys2 INTEGER,dia2 INTEGER,pulse INTEGER,period TEXT,note TEXT,FOREIGN KEY(user_id) REFERENCES users(id))");
        }
        public void onUpgrade(SQLiteDatabase d,int oldV,int newV) {}
        void addUser(String n) { ContentValues v=new ContentValues(); v.put("name",n); getWritableDatabase().insert("users",null,v); }
        void deleteUser(int id) { SQLiteDatabase d=getWritableDatabase(); d.delete("measures","user_id=?",new String[]{""+id}); d.delete("users","id=?",new String[]{""+id}); }
        void deleteMeasure(int id) { getWritableDatabase().delete("measures","id=?",new String[]{""+id}); }
        void addMeasure(int uid,int s1,int d1,int s2,int d2,Integer p,String per,String note) {
            ContentValues v=new ContentValues();
            v.put("user_id",uid);
            v.put("created",new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date()));
            v.put("sys1",s1); v.put("dia1",d1); v.put("sys2",s2); v.put("dia2",d2);
            if(p!=null) v.put("pulse",p);
            v.put("period",per); v.put("note",note);
            getWritableDatabase().insert("measures",null,v);
        }
    }
}

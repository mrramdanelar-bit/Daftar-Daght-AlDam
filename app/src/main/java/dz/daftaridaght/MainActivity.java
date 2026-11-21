package dz.daftaridaght;

import android.app.*;
import android.os.Bundle;
import android.os.Build;
import android.content.pm.PackageManager;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout;
import android.text.TextDirectionHeuristics;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    DB db;
    LinearLayout root;
    int activeUserId = -1;
    String activeUserName = "";
    static final int PDF_REQUEST = 7412;
    static final int OCR_REQUEST = 7413;
    static final int CAMERA_PERMISSION_REQUEST = 7414;
    Uri cameraImageUri = null;
    String pendingPdfText = "";
    TextRecognizer textRecognizer;

    final int GREEN = Color.rgb(23,107,99);
    final int DARK_GREEN = Color.rgb(20,86,79);
    final int BG = Color.rgb(243,247,246);
    final int TEXT_COLOR = Color.rgb(23,43,42);
    final int MUTED = Color.rgb(100,119,115);
    final int BORDER = Color.rgb(225,234,232);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(Color.rgb(20,70,65));
        db = new DB(this);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        showUsers();
    }

    int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    GradientDrawable shape(int color,int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    GradientDrawable cardShape() { GradientDrawable d=shape(Color.WHITE,18); d.setStroke(dp(1),BORDER); return d; }
    TextView text(String value,int size,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(TEXT_COLOR);
        if(bold)t.setTypeface(null,Typeface.BOLD); t.setPadding(0,dp(5),0,dp(5)); return t;
    }
    TextView muted(String value) { TextView t=text(value,13,false); t.setTextColor(MUTED); return t; }
    LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return l; }
    LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return l; }
    LinearLayout card() {
        LinearLayout l=column(); l.setPadding(dp(16),dp(14),dp(16),dp(14)); l.setBackground(cardShape());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(7),0,dp(7)); root.addView(l,p); return l;
    }
    void space(LinearLayout parent,int height) { View v=new View(this); parent.addView(v,new LinearLayout.LayoutParams(1,dp(height))); }
    Button button(String value,boolean primary) {
        Button b=new Button(this); b.setText(value); b.setTextSize(14); b.setTypeface(null,Typeface.BOLD); b.setAllCaps(false);
        b.setTextColor(primary?Color.WHITE:DARK_GREEN); b.setPadding(dp(12),dp(5),dp(12),dp(5));
        b.setBackground(shape(primary?GREEN:Color.rgb(232,241,239),12)); return b;
    }
    EditText edit(String hint,int type) {
        EditText e=new EditText(this); e.setHint(hint); e.setTextSize(15); e.setSingleLine(true); e.setTextColor(TEXT_COLOR);
        e.setHintTextColor(MUTED); e.setInputType(type); e.setPadding(dp(12),dp(10),dp(12),dp(10));
        e.setBackground(shape(Color.WHITE,11)); e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return e;
    }
    void base(String title) {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); root=column();
        root.setPadding(dp(12),dp(12),dp(12),dp(24)); root.setBackgroundColor(BG); scroll.addView(root); setContentView(scroll);
        LinearLayout header=column(); header.setPadding(dp(20),dp(18),dp(20),dp(18)); header.setBackground(shape(GREEN,20)); root.addView(header);
        TextView h=text("دفتر ضغط الدم",25,true); h.setTextColor(Color.WHITE); header.addView(h);
        TextView sub=text(title,15,false); sub.setTextColor(Color.rgb(225,243,239)); header.addView(sub); space(root,10);
    }
    void showUsers() {
        activeUserId=-1; base("متابعة قياساتك المنزلية");
        LinearLayout c=card(); c.addView(text("مرحبًا بك 👋",20,true));
        c.addView(muted("اختر الشخص لفتح سجله الخاص، أو أضف مستخدمًا جديدًا."));
        Button add=button("＋ إضافة مستخدم جديد",true); c.addView(add); add.setOnClickListener(v->addUserDialog());
        root.addView(text("المستخدمون",19,true));
        Cursor cur=db.getReadableDatabase().rawQuery("SELECT id,name FROM users ORDER BY name COLLATE NOCASE",null);
        if(!cur.moveToFirst()) { LinearLayout empty=card(); TextView t=text("لا يوجد مستخدمون بعد.",16,true); t.setGravity(Gravity.CENTER); empty.addView(t); empty.addView(muted("ابدأ بإضافة أول مستخدم.")); }
        else do {
            final int id=cur.getInt(0); final String name=cur.getString(1); LinearLayout item=card(); item.addView(text("👤  "+name,19,true));
            Button open=button("فتح السجل",true); item.addView(open); open.setOnClickListener(v->{activeUserId=id;activeUserName=name;showProfile();});
            Button del=button("حذف المستخدم وسجله",false); item.addView(del);
            del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("حذف المستخدم").setMessage("سيتم حذف جميع قياسات "+name+". هل تريد المتابعة؟")
                .setNegativeButton("إلغاء",null).setPositiveButton("حذف",(d,w)->{db.deleteUser(id);showUsers();}).show());
        } while(cur.moveToNext());
        cur.close();
    }
    void addUserDialog() {
        EditText name=edit("اكتب اسم المستخدم",InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this).setTitle("إضافة مستخدم جديد").setView(name).setNegativeButton("إلغاء",null)
            .setPositiveButton("حفظ",(d,w)->{String n=name.getText().toString().trim();if(n.isEmpty()){toast("يرجى كتابة الاسم");return;}db.addUser(n);showUsers();}).show();
    }

    void showProfile() {
        base("سجل: "+activeUserName);
        Button back=button("← العودة إلى المستخدمين",false); root.addView(back); back.setOnClickListener(v->showUsers());
        Button pdf=button("📄 تصدير تقرير PDF",true); root.addView(pdf); pdf.setOnClickListener(v->exportPdf());
        LinearLayout form=card(); form.addView(text("تسجيل قياس جديد",20,true));
        form.addView(muted("أدخل قراءة الضغط المسجلة من جهازك أو استخرجها من صورة."));
        Button scan=button("📷 القياس الذكي بالكاميرا",true); form.addView(scan); scan.setOnClickListener(v->chooseMeasurementImage());
        EditText sys1=edit("الانقباضي SYS - مثال 120",InputType.TYPE_CLASS_NUMBER);
        EditText dia1=edit("الانبساطي DIA - مثال 80",InputType.TYPE_CLASS_NUMBER);
        EditText sys2=edit("الانقباضي - قراءة ثانية اختيارية",InputType.TYPE_CLASS_NUMBER);
        EditText dia2=edit("الانبساطي - قراءة ثانية اختيارية",InputType.TYPE_CLASS_NUMBER);
        EditText pulse=edit("النبض (اختياري)",InputType.TYPE_CLASS_NUMBER);
        EditText note=edit("ملاحظات (اختياري)",InputType.TYPE_CLASS_TEXT);
        form.addView(text("القراءة الأولى",16,true)); form.addView(sys1); space(form,7); form.addView(dia1);
        space(form,10); form.addView(text("القراءة الثانية (اختيارية)",16,true)); form.addView(sys2); space(form,7); form.addView(dia2);
        space(form,10); form.addView(pulse); space(form,7); form.addView(note);
        Spinner period=new Spinner(this); String[] periods={"صباحًا","مساءً","وقت آخر"};
        period.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,periods));
        space(form,8); form.addView(text("فترة القياس",14,true)); form.addView(period);
        TextView average=text("",17,true); average.setTextColor(GREEN); form.addView(average);
        TextWatcherHelper watcher=new TextWatcherHelper(sys1,dia1,sys2,dia2,average); watcher.attach();
        Button save=button("حفظ القياس",true); form.addView(save);
        save.setOnClickListener(v->{try{
            String aText=sys1.getText().toString().trim(),bText=dia1.getText().toString().trim();
            String cText=sys2.getText().toString().trim(),dText=dia2.getText().toString().trim();
            if(aText.isEmpty()||bText.isEmpty()){toast("أدخل القراءة الأولى");return;}
            int a=Integer.parseInt(aText),b=Integer.parseInt(bText); Integer c=null,d=null;
            if(!cText.isEmpty()||!dText.isEmpty()){if(cText.isEmpty()||dText.isEmpty()){toast("أكمل القراءة الثانية أو اتركها فارغة");return;}c=Integer.parseInt(cText);d=Integer.parseInt(dText);}
            if(!valid(a,b)||(c!=null&&!valid(c,d))){toast("تحقق من قيم الضغط المدخلة");return;}
            Integer p=null;String pt=pulse.getText().toString().trim();if(!pt.isEmpty()){p=Integer.parseInt(pt);if(p<20||p>250){toast("تحقق من قيمة النبض");return;}}
            db.addMeasure(activeUserId,a,b,c,d,p,period.getSelectedItem().toString(),note.getText().toString().trim());toast("تم حفظ القياس بنجاح");showProfile();
        }catch(Exception ex){toast("يرجى التأكد من إدخال الأرقام بصورة صحيحة");}});
        root.addView(text("سجل القياسات",20,true));
        Cursor cur=db.getReadableDatabase().rawQuery("SELECT id,created,sys1,dia1,sys2,dia2,pulse,period,note FROM readings WHERE user_id=? ORDER BY id DESC",new String[]{String.valueOf(activeUserId)});
        if(!cur.moveToFirst()){LinearLayout empty=card();TextView t=text("لا توجد قياسات مسجلة بعد.",15,true);t.setGravity(Gravity.CENTER);empty.addView(t);}
        else do {
            final int id=cur.getInt(0);String date=cur.getString(1);int s1=cur.getInt(2),d1=cur.getInt(3);
            Integer s2=cur.isNull(4)?null:cur.getInt(4),d2=cur.isNull(5)?null:cur.getInt(5);
            String p=cur.isNull(6)?"غير مسجل":String.valueOf(cur.getInt(6));String per=cur.getString(7),nt=cur.getString(8);
            LinearLayout item=card();item.addView(text(date+"  •  "+per,14,true));String reading=s1+"/"+d1;if(s2!=null)reading+="  |  "+s2+"/"+d2;
            TextView values=text(reading+" mmHg",23,true);values.setTextColor(GREEN);item.addView(values);
            if(s2!=null)item.addView(muted("متوسط القراءتين: "+Math.round((s1+s2)/2.0)+" / "+Math.round((d1+d2)/2.0)));
            item.addView(muted("النبض: "+p));if(nt!=null&&!nt.isEmpty())item.addView(muted("ملاحظة: "+nt));
            Button edit=button("✏️ تعديل القياس",false);item.addView(edit);edit.setOnClickListener(v->editMeasureDialog(id,s1,d1,s2,d2,p,per,nt));
            Button delete=button("حذف القياس",false);item.addView(delete);delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("هل تريد حذف هذا القياس؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(dd,w)->{db.deleteMeasure(id);showProfile();}).show());
        } while(cur.moveToNext());
        cur.close();
        LinearLayout reminder=card();reminder.addView(text("تذكير للقياس الصحيح",17,true));reminder.addView(muted("• استرح بهدوء 5 دقائق قبل القياس.\n• اجلس وظهرك مسنود وقدماك على الأرض.\n• ضع السوار على الذراع بمستوى القلب.\n• تجنب القهوة والتدخين والرياضة خلال 30 دقيقة قبل القياس."));
    }

    // اختيار صورة من المعرض أو التقاط صورة كاملة الدقة بالكاميرا
    void chooseMeasurementImage() {
        new AlertDialog.Builder(this)
            .setTitle("القياس الذكي بالكاميرا")
            .setItems(new String[]{"📷 التقاط صورة الآن", "🖼️ اختيار صورة من المعرض"}, (dialog, which) -> {
                if (which == 0) launchCameraCapture();
                else launchGalleryPicker();
            }).setNegativeButton("إلغاء", null).show();
    }
    void launchGalleryPicker() {
        try {
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
            pick.setType("image/*");
            startActivityForResult(Intent.createChooser(pick, "اختر صورة جهاز الضغط"), OCR_REQUEST);
        } catch (Exception e) { toast("تعذر فتح معرض الصور"); }
    }
    void launchCameraCapture() {
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, CAMERA_PERMISSION_REQUEST);
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "DaftarDaght_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT >= 29) values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DaftarDaght");
            cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (cameraImageUri == null) { toast("تعذر تجهيز مكان حفظ الصورة"); return; }
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(camera, OCR_REQUEST);
        } catch (Exception e) { cameraImageUri = null; toast("تعذر فتح الكاميرا"); }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) launchCameraCapture();
            else toast("يلزم السماح بحفظ الصورة لالتقاط صورة كاملة الدقة");
        }
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==OCR_REQUEST&&resultCode!=RESULT_OK){ cameraImageUri=null; }
        if(requestCode==OCR_REQUEST&&resultCode==RESULT_OK&&(data!=null||cameraImageUri!=null)){
            try {
                Bitmap bitmap=null;
                Uri imageUri = data == null ? null : data.getData();
                if (imageUri == null && cameraImageUri != null) imageUri = cameraImageUri;
                if (imageUri != null) {
                    InputStream in=getContentResolver().openInputStream(imageUri);
                    if(in!=null){bitmap=android.graphics.BitmapFactory.decodeStream(in);in.close();}
                }
                if(bitmap==null && data!=null && data.getExtras()!=null) {
                    Object extra=data.getExtras().get("data");
                    if(extra instanceof Bitmap) bitmap=(Bitmap)extra;
                }
                cameraImageUri = null;
                if(bitmap==null){toast("لم نتمكن من قراءة الصورة. حاول التقاطها من جديد.");return;}
                toast("جارٍ قراءة الأرقام من الصورة...");
                InputImage image=InputImage.fromBitmap(bitmap,0);
                textRecognizer.process(image).addOnSuccessListener(result->{
                    int[] values=parseOcrValues(result);
                    if(values[0]<=0||values[1]<=0){showOcrReview("",0,0,values[2]);}
                    else showOcrReview(result.getText(),values[0],values[1],values[2]);
                }).addOnFailureListener(e->toast("تعذر التعرف على الأرقام. جرّب صورة أوضح."));
            } catch(Exception e){toast("حدث خطأ أثناء فتح الصورة");}
        }
        if(requestCode==PDF_REQUEST&&resultCode==RESULT_OK&&data!=null){Uri uri=data.getData();if(uri==null){toast("لم يتم اختيار مكان الحفظ");return;}try{createPdf(uri,pendingPdfText);toast("تم إنشاء تقرير PDF بنجاح");}catch(Exception e){toast("تعذر إنشاء ملف PDF");}}
    }

    // يحاول أولاً ربط الأرقام بعناوين SYS و DIA و PUL، ثم يستخدم ترتيب الشاشة عند غياب العناوين.
    
    
int[] parseOcrValues(Text result) {
    int sys = 0, dia = 0, pulse = 0;

    // كل عنصر: موضع Y، ترتيب الظهور، القيمة، نوع العنوان
    // 1 = SYS، 2 = DIA، 3 = PULSE، 0 = رقم دون عنوان
    ArrayList<int[]> numbers = new ArrayList<>();
    int order = 0;

    Pattern numberPattern = Pattern.compile("[0-9]{2,3}");

    for (Text.TextBlock block : result.getTextBlocks()) {
        for (Text.Line line : block.getLines()) {
            String raw = normalizeDigits(line.getText())
                    .toUpperCase(Locale.ROOT);

            android.graphics.Rect bounds = line.getBoundingBox();
            int y = bounds == null ? order * 1000 :
                    (bounds.top + bounds.bottom) / 2;

            int label = 0;

            if (raw.matches("(?s).*\\bSYS\\b.*") ||
                raw.contains("SYSTOLIC")) {
                label = 1;
            } else if (raw.matches("(?s).*\\bDIA\\b.*") ||
                       raw.contains("DIASTOLIC")) {
                label = 2;
            } else if (raw.matches("(?s).*\\bPUL\\b.*") ||
                       raw.contains("PULSE") ||
                       raw.matches("(?s).*\\bPR\\b.*")) {
                label = 3;
            }

            Matcher matcher = numberPattern.matcher(raw);

            while (matcher.find()) {
                int value = parseInt(matcher.group());

                if (value >= 20 && value <= 280) {
                    numbers.add(new int[]{
                        y, order++, value, label
                    });
                }
            }
        }
    }

    // ترتيب الأرقام حسب ارتفاعها في الصورة
    Collections.sort(numbers, (a, b) -> {
        if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
        return Integer.compare(a[1], b[1]);
    });

    // استخراج القيم التي تحمل عناوين واضحة
    for (int[] item : numbers) {
        int value = item[2];

        if (item[3] == 1 && value >= 50 && value <= 280)
            sys = value;

        if (item[3] == 2 && value >= 30 && value <= 180)
            dia = value;

        if (item[3] == 3 && value >= 20 && value <= 250)
            pulse = value;
    }

    // عند غياب العناوين نعتمد ترتيب الشاشة:
    // الأعلى SYS، الوسط DIA، الأسفل PULSE
    int lastValue = -1;

    for (int[] item : numbers) {
        int value = item[2];

        if (value == lastValue) continue;
        lastValue = value;

        if (item[3] != 0) continue;

        if (sys == 0 && value >= 50 && value <= 280) {
            sys = value;
        } else if (dia == 0 && value >= 30 && value <= 180) {
            dia = value;
        } else if (pulse == 0 && value >= 20 && value <= 250) {
            pulse = value;
        }
    }

    if (sys < 50 || sys > 280) sys = 0;
    if (dia < 30 || dia > 180) dia = 0;
    if (pulse < 20 || pulse > 250) pulse = 0;

    return new int[]{sys, dia, pulse};
}

    int parseInt(String s){try{return Integer.parseInt(s);}catch(Exception e){return 0;}}
    String normalizeDigits(String s){return s.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9');}

    void showOcrReview(String recognized,int sysValue,int diaValue,int pulseValue) {
        LinearLayout layout=column();layout.setPadding(dp(15),dp(8),dp(15),dp(8));
        TextView info=text("راجع الأرقام المستخرجة وعدّلها إن لزم قبل الحفظ.",14,true);layout.addView(info);
        EditText sys=edit("الضغط الانقباضي SYS",InputType.TYPE_CLASS_NUMBER);EditText dia=edit("الضغط الانبساطي DIA",InputType.TYPE_CLASS_NUMBER);EditText pulse=edit("النبض PULSE (اختياري)",InputType.TYPE_CLASS_NUMBER);
        if(sysValue>0)sys.setText(String.valueOf(sysValue));if(diaValue>0)dia.setText(String.valueOf(diaValue));if(pulseValue>0)pulse.setText(String.valueOf(pulseValue));
        layout.addView(text("SYS – الانقباضي",14,true));layout.addView(sys);layout.addView(text("DIA – الانبساطي",14,true));layout.addView(dia);layout.addView(text("PULSE – النبض",14,true));layout.addView(pulse);
        if(recognized==null||recognized.trim().isEmpty()||sysValue==0||diaValue==0){TextView warn=muted("لم يتم التعرف على جميع القيم بثقة. أدخلها يدويًا بعد النظر إلى شاشة الجهاز.");warn.setTextColor(Color.rgb(180,90,30));layout.addView(warn);}
        ScrollView sc=new ScrollView(this);sc.addView(layout);
        new AlertDialog.Builder(this).setTitle("مراجعة القياس المستخرج").setView(sc).setNegativeButton("إلغاء",null).setPositiveButton("تأكيد وحفظ",(dialog,which)->{
            try{
                String st=sys.getText().toString().trim(),dt=dia.getText().toString().trim(),pt=pulse.getText().toString().trim();
                if(st.isEmpty()||dt.isEmpty()){toast("أدخل الانقباضي والانبساطي");return;}
                int s=Integer.parseInt(st),d=Integer.parseInt(dt);if(!valid(s,d)){toast("القيم غير منطقية؛ راجع الأرقام على شاشة الجهاز");return;}
                Integer p=null;if(!pt.isEmpty()){p=Integer.parseInt(pt);if(p<20||p>250){toast("تحقق من قيمة النبض");return;}}
                db.addMeasure(activeUserId,s,d,null,null,p,"وقت آخر","تم إدخال القياس من صورة");
                toast("تم حفظ القياس المستخرج بنجاح");showProfile();
            }catch(Exception e){toast("تحقق من الأرقام المدخلة");}
        }).show();
    }

    boolean valid(int s,int d){return s>=50&&s<=280&&d>=30&&d<=180&&s>d;}
    void editMeasureDialog(int id,int s1,int d1,Integer s2,Integer d2,String pulseValue,String per,String noteValue){
        LinearLayout layout=column();layout.setPadding(dp(15),dp(8),dp(15),dp(8));
        EditText e1=edit("الانقباضي SYS",InputType.TYPE_CLASS_NUMBER),e2=edit("الانبساطي DIA",InputType.TYPE_CLASS_NUMBER);
        EditText e3=edit("الانقباضي - القراءة الثانية اختيارية",InputType.TYPE_CLASS_NUMBER),e4=edit("الانبساطي - القراءة الثانية اختيارية",InputType.TYPE_CLASS_NUMBER);
        EditText ep=edit("النبض (اختياري)",InputType.TYPE_CLASS_NUMBER),en=edit("ملاحظات",InputType.TYPE_CLASS_TEXT);
        e1.setText(String.valueOf(s1));e2.setText(String.valueOf(d1));if(s2!=null)e3.setText(String.valueOf(s2));if(d2!=null)e4.setText(String.valueOf(d2));if(!pulseValue.equals("غير مسجل"))ep.setText(pulseValue);en.setText(noteValue);
        layout.addView(text("القراءة الأولى",15,true));layout.addView(e1);layout.addView(e2);space(layout,8);layout.addView(text("القراءة الثانية الاختيارية",15,true));layout.addView(e3);layout.addView(e4);space(layout,8);layout.addView(ep);layout.addView(en);
        Spinner sp=new Spinner(this);String[] periods={"صباحًا","مساءً","وقت آخر"};sp.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,periods));for(int i=0;i<periods.length;i++)if(periods[i].equals(per))sp.setSelection(i);layout.addView(sp);
        ScrollView scroll=new ScrollView(this);scroll.addView(layout);
        new AlertDialog.Builder(this).setTitle("تعديل القياس").setView(scroll).setNegativeButton("إلغاء",null).setPositiveButton("حفظ التعديلات",(dialog,which)->{try{
            int a=Integer.parseInt(e1.getText().toString().trim()),b=Integer.parseInt(e2.getText().toString().trim());String ct=e3.getText().toString().trim(),dt=e4.getText().toString().trim();Integer c=null,d=null;
            if(!ct.isEmpty()||!dt.isEmpty()){if(ct.isEmpty()||dt.isEmpty()){toast("أكمل القراءة الثانية أو اتركها فارغة");return;}c=Integer.parseInt(ct);d=Integer.parseInt(dt);}
            if(!valid(a,b)||(c!=null&&!valid(c,d))){toast("تحقق من قيم الضغط");return;}Integer p=null;String pv=ep.getText().toString().trim();if(!pv.isEmpty()){p=Integer.parseInt(pv);if(p<20||p>250){toast("تحقق من قيمة النبض");return;}}
            db.updateMeasure(id,a,b,c,d,p,sp.getSelectedItem().toString(),en.getText().toString().trim());toast("تم تعديل القياس");showProfile();
        }catch(Exception ex){toast("يرجى التأكد من صحة الأرقام");}}).show();
    }

    void exportPdf(){
        Cursor c=db.getReadableDatabase().rawQuery("SELECT created,sys1,dia1,sys2,dia2,pulse,period,note FROM readings WHERE user_id=? ORDER BY id DESC",new String[]{String.valueOf(activeUserId)});
        if(!c.moveToFirst()){c.close();toast("لا توجد قياسات لتصديرها");return;}
        int count=0;double sumSys=0,sumDia=0,sumPulse=0;int pulseCount=0;StringBuilder report=new StringBuilder();
        report.append("دفتر ضغط الدم\nتقرير قياسات المستخدم: ").append(activeUserName).append("\nتاريخ إنشاء التقرير: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date())).append("\n========================================\n\n");
        do{
            String date=c.getString(0);int s1=c.getInt(1),d1=c.getInt(2);Integer s2=c.isNull(3)?null:c.getInt(3),d2=c.isNull(4)?null:c.getInt(4),pulse=c.isNull(5)?null:c.getInt(5);String period=c.isNull(6)?"":c.getString(6),note=c.isNull(7)?"":c.getString(7);
            double avgS=s2==null?s1:(s1+s2)/2.0,avgD=d2==null?d1:(d1+d2)/2.0;sumSys+=avgS;sumDia+=avgD;count++;
            report.append("القياس رقم ").append(count).append("\nالتاريخ والوقت: ").append(date).append("\nالفترة: ").append(period).append("\nالقراءة الأولى: ").append(s1).append(" / ").append(d1).append(" mmHg\n");
            if(s2!=null)report.append("القراءة الثانية: ").append(s2).append(" / ").append(d2).append(" mmHg\nمتوسط القراءتين: ").append(Math.round(avgS)).append(" / ").append(Math.round(avgD)).append(" mmHg\n");
            if(pulse!=null){report.append("النبض: ").append(pulse).append(" نبضة/دقيقة\n");sumPulse+=pulse;pulseCount++;}else report.append("النبض: غير مسجل\n");
            if(!note.isEmpty())report.append("ملاحظات: ").append(note).append("\n");report.append("----------------------------------------\n\n");
        }while(c.moveToNext());c.close();
        report.append("\nملخص وإحصائيات المستخدم\nعدد القياسات: ").append(count).append("\nمتوسط الضغط الانقباضي: ").append(Math.round(sumSys/count)).append(" mmHg\nمتوسط الضغط الانبساطي: ").append(Math.round(sumDia/count)).append(" mmHg\n");
        if(pulseCount>0)report.append("متوسط النبض: ").append(Math.round(sumPulse/pulseCount)).append(" نبضة/دقيقة\n");else report.append("متوسط النبض: لا توجد بيانات\n");
        report.append("\nهذا التقرير يعرض القياسات المسجلة في التطبيق ولا يُعد تشخيصًا طبيًا.");pendingPdfText=report.toString();
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/pdf");intent.putExtra(Intent.EXTRA_TITLE,"تقرير ضغط الدم - "+activeUserName+".pdf");
        try{startActivityForResult(intent,PDF_REQUEST);}catch(Exception e){toast("تعذر فتح نافذة حفظ ملف PDF");}
    }
    void createPdf(Uri uri,String content)throws IOException{
        PdfDocument document=new PdfDocument();TextPaint paint=new TextPaint();paint.setColor(Color.BLACK);paint.setTextSize(12);paint.setAntiAlias(true);
        int pageWidth=595,pageHeight=842,margin=42,contentWidth=pageWidth-margin*2;
        StaticLayout layout=StaticLayout.Builder.obtain(content,0,content.length(),paint,contentWidth).setAlignment(Layout.Alignment.ALIGN_OPPOSITE).setTextDirection(TextDirectionHeuristics.RTL).setLineSpacing(5,1.15f).setIncludePad(true).build();
        int usableHeight=pageHeight-margin*2,totalHeight=layout.getHeight(),pages=Math.max(1,(int)Math.ceil(totalHeight/(double)usableHeight));
        for(int i=0;i<pages;i++){
            PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(pageWidth,pageHeight,i+1).create();PdfDocument.Page page=document.startPage(info);Canvas canvas=page.getCanvas();canvas.drawColor(Color.WHITE);canvas.save();canvas.clipRect(margin,margin,pageWidth-margin,pageHeight-margin);canvas.translate(margin,margin-i*usableHeight);layout.draw(canvas);canvas.restore();
            TextPaint footer=new TextPaint();footer.setColor(Color.GRAY);footer.setTextSize(9);footer.setAntiAlias(true);canvas.drawText("Daftar Daght AlDam - Page "+(i+1)+" / "+pages,margin,pageHeight-18,footer);document.finishPage(page);
        }
        OutputStream out=null;try{out=getContentResolver().openOutputStream(uri);if(out==null)throw new IOException("تعذر فتح الملف");document.writeTo(out);}finally{if(out!=null)out.close();document.close();}
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    class TextWatcherHelper{
        EditText a,b,c,d;TextView result;TextWatcherHelper(EditText a,EditText b,EditText c,EditText d,TextView result){this.a=a;this.b=b;this.c=c;this.d=d;this.result=result;}
        void attach(){android.text.TextWatcher w=new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){calculate();}public void afterTextChanged(android.text.Editable e){}};a.addTextChangedListener(w);b.addTextChangedListener(w);c.addTextChangedListener(w);d.addTextChangedListener(w);}
        void calculate(){try{String av=a.getText().toString().trim(),bv=b.getText().toString().trim(),cv=c.getText().toString().trim(),dv=d.getText().toString().trim();if(av.isEmpty()||bv.isEmpty()||cv.isEmpty()||dv.isEmpty()){result.setText("");return;}int x=Integer.parseInt(av),y=Integer.parseInt(bv),z=Integer.parseInt(cv),q=Integer.parseInt(dv);if(!valid(x,y)||!valid(z,q)){result.setText("");return;}result.setText("متوسط القراءتين: "+Math.round((x+z)/2.0)+" / "+Math.round((y+q)/2.0)+" mmHg");}catch(Exception ex){result.setText("");}}
    }
    class DB extends SQLiteOpenHelper{
        DB(Context c){super(c,"pressure_diary_v2.db",null,1);}
        public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");d.execSQL("CREATE TABLE readings(id INTEGER PRIMARY KEY AUTOINCREMENT,user_id INTEGER NOT NULL,created TEXT NOT NULL,sys1 INTEGER NOT NULL,dia1 INTEGER NOT NULL,sys2 INTEGER,dia2 INTEGER,pulse INTEGER,period TEXT,note TEXT,FOREIGN KEY(user_id) REFERENCES users(id))");}
        public void onUpgrade(SQLiteDatabase d,int oldV,int newV){}
        void addUser(String name){ContentValues v=new ContentValues();v.put("name",name);getWritableDatabase().insert("users",null,v);}
        void deleteUser(int id){SQLiteDatabase d=getWritableDatabase();d.delete("readings","user_id=?",new String[]{String.valueOf(id)});d.delete("users","id=?",new String[]{String.valueOf(id)});}
        void deleteMeasure(int id){getWritableDatabase().delete("readings","id=?",new String[]{String.valueOf(id)});}
        void addMeasure(int uid,int s1,int d1,Integer s2,Integer d2,Integer pulse,String period,String note){ContentValues v=new ContentValues();v.put("user_id",uid);v.put("created",new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date()));v.put("sys1",s1);v.put("dia1",d1);if(s2!=null)v.put("sys2",s2);if(d2!=null)v.put("dia2",d2);if(pulse!=null)v.put("pulse",pulse);v.put("period",period);v.put("note",note);getWritableDatabase().insert("readings",null,v);}
        void updateMeasure(int id,int s1,int d1,Integer s2,Integer d2,Integer pulse,String period,String note){ContentValues v=new ContentValues();v.put("sys1",s1);v.put("dia1",d1);if(s2==null)v.putNull("sys2");else v.put("sys2",s2);if(d2==null)v.putNull("dia2");else v.put("dia2",d2);if(pulse==null)v.putNull("pulse");else v.put("pulse",pulse);v.put("period",period);v.put("note",note);getWritableDatabase().update("readings",v,"id=?",new String[]{String.valueOf(id)});}
    }
    @Override protected void onDestroy(){super.onDestroy();if(textRecognizer!=null)textRecognizer.close();}
}
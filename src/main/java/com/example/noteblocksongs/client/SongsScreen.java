package com.example.noteblocksongs.client;

import com.example.noteblocksongs.config.NbsConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SongsScreen extends Screen {
    private final BlockPos pos;
    private TextFieldWidget search;
    private int selected=-1;
    private int scroll=0;
    private float volume=1.0f;
    private List<SongLibrary.Song> visible=List.of();

    public SongsScreen(BlockPos pos){super(Text.literal("Songs"));this.pos=pos;this.volume=NbsConfig.INSTANCE.volume;}

    @Override protected void init(){
        SongLibrary.refresh();
        search=new TextFieldWidget(textRenderer,width/2-150,20,300,20,Text.literal("Search"));
        search.setMaxLength(80);search.setPlaceholder(Text.literal("Search songs..."));search.setChangedListener(s->{scroll=0;refreshVisible();});
        addDrawableChild(search);
        addDrawableChild(ButtonWidget.builder(Text.literal("Play"),b->play()).dimensions(width/2-150,height-32,70,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Stop"),b->NoteBlockSongsClient.requestStop(pos)).dimensions(width/2-74,height-32,70,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Volume: "+Math.round(volume*100)+"%"),b->{volume+=0.25f;if(volume>2.0f)volume=0; b.setMessage(Text.literal("Volume: "+Math.round(volume*100)+"%"));}).dimensions(width/2+2,height-32,90,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"),b->{SongLibrary.refresh();selected=-1;scroll=0;refreshVisible();}).dimensions(width/2+98,height-32,70,20).build());
        refreshVisible();
    }

    private void refreshVisible(){
        String q=search==null?"":search.getText().toLowerCase(Locale.ROOT);visible=SongLibrary.songs().stream().filter(s->q.isBlank()||s.name().toLowerCase(Locale.ROOT).contains(q)).toList();}
    private void play(){if(selected>=0&&selected<visible.size())NoteBlockSongsClient.requestPlay(pos,visible.get(selected),volume);}

    @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
        renderBackground(c,mouseX,mouseY,delta);
        int left=width/2-180,right=width/2+180,top=52,bottom=height-45;
        c.fill(left,top,right,bottom,0xCC10151C);c.drawBorder(left,top,right,bottom,0xFF5A6572);
        c.drawTextWithShadow(textRenderer,Text.literal("Songs"),left+10,35,0xFFFFFF);
        c.drawTextWithShadow(textRenderer,Text.literal(visible.size()+" MP3 file(s)"),right-100,35,0xAAAAAA);
        c.drawTextWithShadow(textRenderer,Text.literal("Playing: "+ActiveSounds.name(pos)),left+10,height-43,0xAFC7DD);
        int rowH=22,maxRows=Math.max(1,(bottom-top)/rowH),maxScroll=Math.max(0,visible.size()-maxRows);scroll=Math.min(scroll,maxScroll);
        for(int i=0;i<maxRows;i++){int idx=i+scroll;if(idx>=visible.size())break;int y=top+i*rowH;boolean sel=idx==selected;c.fill(left+5,y+2,right-5,y+20,sel?0xFF304B63:0xFF1B232D);c.drawTextWithShadow(textRenderer,Text.literal("♪ "+visible.get(idx).name()),left+12,y+7,sel?0xFFFFFF:0xD0D7DE);}
        super.render(c,mouseX,mouseY,delta);
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button==0){int left=width/2-180,top=52,bottom=height-45,rowH=22; if(mx>=left&&mx<=width/2+180&&my>=top&&my<bottom){int idx=(int)((my-top)/rowH)+scroll;if(idx>=0&&idx<visible.size()){selected=idx;return true;}}}
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){scroll-= (int)Math.signum(vertical);scroll=Math.max(0,Math.min(scroll,Math.max(0,visible.size()-Math.max(1,(height-97)/22))));return true;}
    @Override public boolean shouldPause(){return false;}
}

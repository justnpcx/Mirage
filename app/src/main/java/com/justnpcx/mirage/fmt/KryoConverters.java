package com.justnpcx.mirage.fmt;

import androidx.room.TypeConverter;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.justnpcx.mirage.database.SubscriptionBean;
import com.justnpcx.mirage.fmt.http.HttpBean;
import com.justnpcx.mirage.fmt.hysteria.HysteriaBean;
import com.justnpcx.mirage.fmt.internal.ChainBean;
import com.justnpcx.mirage.fmt.mieru.MieruBean;
import com.justnpcx.mirage.fmt.naive.NaiveBean;
import com.justnpcx.mirage.fmt.shadowsocks.ShadowsocksBean;
import moe.matsuri.nb4a.proxy.anytls.AnyTLSBean;
import moe.matsuri.nb4a.proxy.shadowtls.ShadowTLSBean;
import com.justnpcx.mirage.fmt.socks.SOCKSBean;
import com.justnpcx.mirage.fmt.ssh.SSHBean;
import com.justnpcx.mirage.fmt.trojan.TrojanBean;
import com.justnpcx.mirage.fmt.trojan_go.TrojanGoBean;
import com.justnpcx.mirage.fmt.tuic.TuicBean;
import com.justnpcx.mirage.fmt.v2ray.VMessBean;
import com.justnpcx.mirage.fmt.wireguard.WireGuardBean;
import com.justnpcx.mirage.ktx.KryosKt;
import com.justnpcx.mirage.ktx.Logs;
import moe.matsuri.nb4a.proxy.config.ConfigBean;
import moe.matsuri.nb4a.proxy.neko.NekoBean;
import moe.matsuri.nb4a.utils.JavaUtil;

public class KryoConverters {

    private static final byte[] NULL = new byte[0];

    @TypeConverter
    public static byte[] serialize(Serializable bean) {
        if (bean == null) return NULL;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBufferOutput buffer = KryosKt.byteBuffer(out);
        bean.serializeToBuffer(buffer);
        buffer.flush();
        buffer.close();
        return out.toByteArray();
    }

    public static <T extends Serializable> T deserialize(T bean, byte[] bytes) {
        if (bytes == null) return bean;
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        ByteBufferInput buffer = KryosKt.byteBuffer(input);
        try {
            bean.deserializeFromBuffer(buffer);
        } catch (KryoException e) {
            Logs.INSTANCE.w(e);
        }
        bean.initializeDefaultValues();
        return bean;
    }

    @TypeConverter
    public static SOCKSBean socksDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new SOCKSBean(), bytes);
    }

    @TypeConverter
    public static HttpBean httpDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new HttpBean(), bytes);
    }

    @TypeConverter
    public static ShadowsocksBean shadowsocksDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new ShadowsocksBean(), bytes);
    }

    @TypeConverter
    public static ConfigBean configDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new ConfigBean(), bytes);
    }

    @TypeConverter
    public static VMessBean vmessDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new VMessBean(), bytes);
    }

    @TypeConverter
    public static TrojanBean trojanDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new TrojanBean(), bytes);
    }

    @TypeConverter
    public static TrojanGoBean trojanGoDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new TrojanGoBean(), bytes);
    }

    @TypeConverter
    public static MieruBean mieruDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new MieruBean(), bytes);
    }

    @TypeConverter
    public static NaiveBean naiveDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new NaiveBean(), bytes);
    }

    @TypeConverter
    public static HysteriaBean hysteriaDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new HysteriaBean(), bytes);
    }

    @TypeConverter
    public static SSHBean sshDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new SSHBean(), bytes);
    }

    @TypeConverter
    public static WireGuardBean wireguardDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new WireGuardBean(), bytes);
    }

    @TypeConverter
    public static TuicBean tuicDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new TuicBean(), bytes);
    }

    @TypeConverter
    public static ShadowTLSBean shadowTLSDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new ShadowTLSBean(), bytes);
    }

    @TypeConverter
    public static AnyTLSBean anyTLSDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new AnyTLSBean(), bytes);
    }


    @TypeConverter
    public static ChainBean chainDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new ChainBean(), bytes);
    }

    @TypeConverter
    public static NekoBean nekoDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new NekoBean(), bytes);
    }

    @TypeConverter
    public static SubscriptionBean subscriptionDeserialize(byte[] bytes) {
        if (JavaUtil.isEmpty(bytes)) return null;
        return deserialize(new SubscriptionBean(), bytes);
    }

}

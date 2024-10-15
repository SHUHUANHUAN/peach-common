
package com.peach.common.listener;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/15 10:44
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheMessage implements Serializable {
    private static final long serialVersionUID = -6221995438342888610L;

    /**
     * 缓存名称
     */
    private String cacheName;
    /**
     * 缓存key
     */
    private Object key;

    private Integer sender;

}

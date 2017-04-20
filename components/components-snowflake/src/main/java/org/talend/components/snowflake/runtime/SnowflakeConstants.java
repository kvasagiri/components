package org.talend.components.snowflake.runtime;

import org.talend.daikon.i18n.GlobalI18N;
import org.talend.daikon.i18n.I18nMessages;

/**
 * Constants used in {@link org.talend.components.snowflake.runtime} package
 */
public final class SnowflakeConstants {

    private static final I18nMessages i18nMessages = GlobalI18N.getI18nMessageProvider()
            .getI18nMessages(SnowflakeConstants.class);

    public static final String TALEND_DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

    public static final String TALEND_DAFEULT_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    public static final String INCORRECT_SNOWFLAKE_ACCOUNT_MESSAGE = i18nMessages.getMessage("error.incorrectAccount");

    public static final String CONNECTION_SUCCESSFUL_MESSAGE = i18nMessages.getMessage("messages.success");

    public static final String SNOWFLAKE_DRIVER = "com.snowflake.client.jdbc.SnowflakeDriver";

    private SnowflakeConstants() {
        throw new AssertionError();
    }
}

package com.nexaerp.email;

import com.nexaerp.overdue.OverdueDocumentSnapshot;
import com.nexaerp.user.User;

public interface OverdueAlertEmailService {
    void send(User recipient, OverdueDocumentSnapshot document);
}

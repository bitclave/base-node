ps -axf | grep BaseNodeApplicationKt | grep -v grep | awk '{print$2}' | xargs kill -SIGINT


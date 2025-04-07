package dev.luna5ama.trollhack.event.events.render

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.event.NamedProfilerEventBus

sealed class RenderCrosshairEvent : Event {
    class Pre : RenderCrosshairEvent(), EventPosting by NamedProfilerEventBus("pre")
    class Post : RenderCrosshairEvent(), EventPosting by NamedProfilerEventBus("post")
}
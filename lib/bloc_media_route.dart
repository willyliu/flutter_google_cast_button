import 'package:bloc/bloc.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter_google_cast_button/flutter_google_cast_button.dart';

const _STATE_UNAVAILABLE = 1;
const _STATE_UNCONNECTED = 2;
const _STATE_CONNECTING = 3;
const _STATE_CONNECTED = 4;

class MediaRouteBloc extends Bloc<MediaRouteEvent, MediaRouteState> {
  var debugMode = false;

  MediaRouteBloc() : super(NoDeviceAvailable()) {
    on<UpdateRouteStateEvent>((event, emit) {
      switch (event.nativeState) {
        case _STATE_UNCONNECTED:
          emit(Unconnected());
          break;
        case _STATE_CONNECTING:
          emit(Connecting());
          break;
        case _STATE_CONNECTED:
          emit(Connected());
          break;
        case _STATE_UNAVAILABLE:
        default:
          emit(NoDeviceAvailable());
      }
    });
    FlutterGoogleCastButton.castEventStream().listen(
      (event) {
        _printD("MediaRouteBloc listen state changed: $event");
        if (event is int) {
          add(UpdateRouteStateEvent(event));
        }
      },
      onError: (e) {
        add(UpdateRouteStateEvent(1));
      },
    );
  }

  _printD(String message) {
    if (debugMode) {
      print(message);
    }
  }
}

class MediaRouteState extends Equatable {
  @override
  List<Object> get props => ['MediaRouteState'];
}

class NoDeviceAvailable extends MediaRouteState {
  @override
  List<Object> get props => ['NoDeviceAvailable'];
}

class Unconnected extends MediaRouteState {
  @override
  List<Object> get props => ['Unconnected'];
}

class Connected extends MediaRouteState {
  @override
  List<Object> get props => ['Connected'];
}

class Connecting extends MediaRouteState {
  @override
  List<Object> get props => ['Connecting'];
}

class MediaRouteEvent extends Equatable {
  @override
  List<Object> get props => ['MediaRouteEvent'];
}

class UpdateRouteStateEvent extends MediaRouteEvent {
  final int nativeState;

  UpdateRouteStateEvent(this.nativeState);

  @override
  List<Object> get props => ['UpdateRouteStateEvent ${nativeState}'];
}
